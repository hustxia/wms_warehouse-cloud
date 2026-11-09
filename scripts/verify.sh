#!/usr/bin/env bash
# WMS V1.1 F-10：一条命令跑完门禁（编译 → 库体检 → e2e（含并发） → 对账 → 前端 build → 报告）。
#
# 用法：
#   bash scripts/verify.sh                 # 全量；建议先停服务再跑，能顺便验证编译
#   bash scripts/verify.sh --skip-build    # 服务已在跑时用（Windows 下运行中的 jar 会锁住 repackage）
#   bash scripts/verify.sh --negative-test # 额外做负向验证：故意改错一条断言，门禁必须非零退出
#
# 任一环节失败立即非零退出，不继续后续步骤。
# 前置：MySQL 已建库（scripts/db-init.sh），Nacos + 6 服务已启动（scripts/cloud-up.sh --with-web）。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
WEB_DIR="${WEB_DIR:-$ROOT/../wms_warehouse-web}"
GATEWAY="${GATEWAY_URL:-http://127.0.0.1:8100}"
SKIP_BUILD=0
NEGATIVE=0
WITH_UI=0
for arg in "$@"; do
  case "$arg" in
    --skip-build) SKIP_BUILD=1 ;;
    --negative-test) NEGATIVE=1 ;;
    --with-ui) WITH_UI=1 ;;
  esac
done

# 本机 mvn 不在 PATH（在 D:/tool/apache-maven-3.6.3/bin），做一次探测
MVN="${MVN:-mvn}"
if ! command -v "$MVN" >/dev/null 2>&1; then
  for candidate in "/d/tool/apache-maven-3.6.3/bin/mvn" "/c/Program Files/apache-maven-3.6.3/bin/mvn"; do
    if [ -f "$candidate" ]; then MVN="$candidate"; break; fi
  done
fi

MYSQL_BIN="${MYSQL_BIN:-/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root}"
DB_NAME="${DB_NAME:-wms_warehouse}"
export MYSQL_PWD="$DB_PASS"
MYSQL_ARGS=("-u$DB_USER" "--default-character-set=utf8mb4" "--database=$DB_NAME" "-B" "-N")
export DB_PASSWORD="${DB_PASSWORD:-root}"
export APP_INTERNAL_TOKEN="${APP_INTERNAL_TOKEN:-wms-internal}"

PASS_COUNT=0
START=$(date +%s)

section() {
  echo ""
  echo "==================== $1 ===================="
}
pass() {
  PASS_COUNT=$((PASS_COUNT + 1))
  echo "  PASS  $1"
}

section "1/6 编译"
if [ "$SKIP_BUILD" = "1" ]; then
  echo "  --skip-build：跳过（服务运行中，jar 被占用时 repackage 会失败）"
else
  if netstat -ano 2>/dev/null | grep -Eq ":81(4[1-5]|00) .*LISTENING"; then
    echo "  检测到服务正在运行，跳过编译以避免 jar 文件锁；如需完整编译请先 cloud-down.sh"
    echo "  （本次编译证据请用停服后的 mvn -o package -DskipTests 单独产出）"
  else
    build_log="$(mktemp)"
    if ( cd "$ROOT" && "$MVN" -o package -DskipTests ) > "$build_log" 2>&1; then
      tail -20 "$build_log"
      if ! grep -q "BUILD SUCCESS" "$build_log"; then
        echo "  编译输出里没有 BUILD SUCCESS" >&2
        rm -f "$build_log"
        exit 1
      fi
      rm -f "$build_log"
      pass "mvn -o package 7 模块 BUILD SUCCESS"
    else
      tail -60 "$build_log" >&2
      rm -f "$build_log"
      echo "  编译失败" >&2
      exit 1
    fi
  fi
fi

section "2/6 库表体检"
[ -f "$MYSQL_BIN" ] || { echo "  找不到 mysql 客户端：$MYSQL_BIN" >&2; exit 1; }
table_count="$("$MYSQL_BIN" "${MYSQL_ARGS[@]}" -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';")"
echo "  表数量：$table_count"
[ "$table_count" -ge 21 ] || { echo "  业务表不足 21 张，先跑 scripts/db-init.sh" >&2; exit 1; }
expr="$("$MYSQL_BIN" "${MYSQL_ARGS[@]}" -e \
  "SELECT GENERATION_EXPRESSION FROM information_schema.columns
   WHERE table_schema='$DB_NAME' AND table_name='wms_inventory' AND column_name='qty_available';")"
echo "  qty_available = $expr"
case "$expr" in
  *qty_locked*) pass "生成列口径含 qty_locked" ;;
  *) echo "  生成列仍是旧口径" >&2; exit 1 ;;
esac
dict_rows="$("$MYSQL_BIN" "${MYSQL_ARGS[@]}" -e "SELECT COUNT(*) FROM sys_dict_data;")"
echo "  sys_dict_data 行数：$dict_rows"
[ "$dict_rows" -ge 1 ] || { echo "  字典未初始化，页面会弹「请求失败」" >&2; exit 1; }
pass "字典种子已就绪"

section "3/6 网关可达"
code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$GATEWAY/wms_inbound_order/1001" || echo 000)
echo "  探测 $GATEWAY/wms_inbound_order/1001 -> HTTP $code"
[ "$code" != "000" ] || {
  echo "  网关不可达。请在你自己的 Git Bash 前台运行：" >&2
  echo "    bash scripts/cloud-up.sh --with-web" >&2
  exit 1
}
pass "网关可达"

section "4/6 e2e（V1.0 全部断言 + V1.1 并发验收）"
mkdir -p "$WEB_DIR/public"
# e2e 把并发结果写成前端 public/ 下的报告，库存页只读取、不自行推导
( cd "$ROOT" && E2E_REPORT_FILE="$WEB_DIR/public/wms-verify-report.json" node scripts/e2e-cloud.mjs )
[ -f "$WEB_DIR/public/wms-verify-report.json" ] || { echo "  验收报告未生成" >&2; exit 1; }
pass "e2e 全部断言通过（含 100 / 150 并发），报告已写入前端 public/"

section "5/6 对账与冲销"
( cd "$ROOT" && node scripts/reconcile.mjs --selftest )
pass "造差异 → 冲销（含幂等）→ 0 条差异"
( cd "$ROOT" && node scripts/reconcile.mjs )
pass "全库对账 0 条差异"

section "6/6 前端构建"
[ -d "$WEB_DIR" ] || { echo "  前端目录不存在：$WEB_DIR" >&2; exit 1; }
( cd "$WEB_DIR" && NODE_OPTIONS= npm run build ) 2>&1 | tail -20
pass "前端 build 成功"

if [ "$WITH_UI" = "1" ]; then
  section "6.5 UI 实机走查（4 个业务页 console / 只读控件）"
  ui_code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "http://127.0.0.1:9040/" || echo 000)
  [ "$ui_code" = "200" ] || { echo "  前端 dev server 9040 不可达，请先跑 cloud-up.sh --with-web" >&2; exit 1; }
  ui_token="verify-ui-$$-$(date +%s)"
  "$MYSQL_BIN" "${MYSQL_ARGS[@]}" -e \
    "INSERT INTO sys_session (token, username, login_ip, expire_time)
     VALUES ('$ui_token', 'admin', '127.0.0.1', DATE_ADD(NOW(), INTERVAL 1 HOUR));"
  ui_rc=0
  ( cd "$ROOT" && TOKEN="$ui_token" node scripts/ui-check-pages.mjs ) || ui_rc=$?
  "$MYSQL_BIN" "${MYSQL_ARGS[@]}" -e "DELETE FROM sys_session WHERE token='$ui_token';"
  [ "$ui_rc" = "0" ] || { echo "  UI 实机走查失败（rc=$ui_rc）" >&2; exit 1; }
  pass "4/4 业务页 console 零 error，只读控件生效"
fi

if [ "$NEGATIVE" = "1" ]; then
  section "负向验证：故意改错一条并发断言，门禁必须失败"
  # 必须落在 scripts/ 下且保留 .mjs 后缀：放 /tmp 会被 Node 当 CommonJS 解析，
  # 那样失败原因是语法错误而不是断言失败，负向验证就成了假阳性。
  tmp="$ROOT/scripts/.e2e-negative-$$.mjs"
  sed 's/eq(succeeded.length, 100,/eq(succeeded.length, 99,/' \
    "$ROOT/scripts/e2e-cloud.mjs" > "$tmp"
  if ( cd "$ROOT" && node "$tmp" >/dev/null 2>&1 ); then
    rm -f "$tmp"
    echo "  负向验证失败：改错的断言竟然通过了" >&2
    exit 1
  fi
  rm -f "$tmp"
  pass "改错断言后脚本非零退出，门禁有效"
fi

section "报告"
echo "  通过环节：$PASS_COUNT"
echo "  耗时：$(( $(date +%s) - START ))s"
echo "  编译：$( [ "$SKIP_BUILD" = "1" ] && echo '已按参数跳过' || echo '已执行/按端口占用跳过' )"
echo "  接口：e2e（网关 + 直连 + MySQL 双向核对）"
echo "  UI：前端 build（页面 console 抓取另见 S8 记录）"
echo "VERIFY PASS"
