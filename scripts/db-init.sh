#!/usr/bin/env bash
# WMS V1.1 F-07：一键建库（空库可跑、重复跑不报错）。
#
# 用法：
#   bash scripts/db-init.sh
#   DB_PASS=xxx DB_USER=xxx DB_NAME=xxx bash scripts/db-init.sh
#   MYSQL_BIN=/path/to/mysql.exe bash scripts/db-init.sh
#   bash scripts/db-init.sh --with-legacy-seed   # 额外导入代码生成器的随机演示数据（默认跳过）
#
# 为什么必须显式 --default-character-set=utf8mb4（坑库 mysql.md）：
#   Windows mysql.exe 默认客户端字符集是 gbk，中文种子会报
#   ERROR 1366 Incorrect string value: '\x80\x81'，字典接口随后返回空数组，
#   每个带字典的页面都弹「请求失败」。
#
# 为什么 --database 而不是依赖 SQL 里的 USE：
#   历史事故：V1.0__init.sql 里的 `USE gen_platform` 覆盖了命令行 --database，
#   把 DROP 打到了开发库上。本脚本所有 SQL 都用 --database 指定目标库，
#   且只对 CREATE DATABASE IF NOT EXISTS 使用无库连接。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-root}"
DB_NAME="${DB_NAME:-wms_warehouse}"
WITH_LEGACY=0
for arg in "$@"; do
  case "$arg" in
    --with-legacy-seed) WITH_LEGACY=1 ;;
  esac
done

if [ -n "${MYSQL_BIN:-}" ]; then
  MYSQL="$MYSQL_BIN"
elif command -v mysql >/dev/null 2>&1; then
  MYSQL="mysql"
elif [ -f "/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" ]; then
  MYSQL="/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe"
else
  echo "找不到 mysql 客户端，请用 MYSQL_BIN=<绝对路径> 指定" >&2
  exit 1
fi

# 用 MYSQL_PWD 传口令，避免 mysql 每次都往 stderr 打安全警告干扰判定
export MYSQL_PWD="$DB_PASS"
MYSQL_ARGS=("-u$DB_USER" "--default-character-set=utf8mb4")

echo "建库：$DB_NAME（客户端 $MYSQL）"
"$MYSQL" "${MYSQL_ARGS[@]}" -e \
  "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

run_sql() {
  local file="$1"
  if [ ! -f "$file" ]; then
    echo "  缺少脚本：$file" >&2
    return 1
  fi
  echo "  -> ${file#$ROOT/}"
  "$MYSQL" "${MYSQL_ARGS[@]}" --database="$DB_NAME" < "$file"
}

echo "[1/3] 基础表结构与登录/菜单/字典"
run_sql "$ROOT/db/init_tables.sql"
run_sql "$ROOT/db/init_sys_user.sql"
run_sql "$ROOT/db/init_sys_flow.sql"

# 生成器 V7 的 init_menu.sql 会写 sys_role_menu.deleted，而骨架 init_sys_user.sql
# 建这张表时只有 (role_id, menu_id) 两列 —— 全新库上第 70 行就报
# ERROR 1054 Unknown column 'deleted'。用 information_schema + PREPARE 做幂等补列，
# 不改生成器产物（改了下次重新生成会被覆盖），已有库重复跑也不会加第二次。
"$MYSQL" "${MYSQL_ARGS[@]}" --database="$DB_NAME" -e "
SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE sys_role_menu ADD COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT ''逻辑删除''',
    'SELECT 1')
  FROM information_schema.columns
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role_menu' AND COLUMN_NAME = 'deleted');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
" >/dev/null
echo "  -> sys_role_menu.deleted 兼容列已确认"

# init_menu.sql 是无条件 INSERT，重复执行会长出第二套菜单。
# 只在菜单表为空时导入，保证「重复跑不报错、也不产生脏数据」。
menu_count="$("$MYSQL" "${MYSQL_ARGS[@]}" --database="$DB_NAME" -B -N -e \
  "SELECT COUNT(*) FROM sys_menu;")"
if [ "$menu_count" = "0" ]; then
  run_sql "$ROOT/db/init_menu.sql"
else
  echo "  -> db/init_menu.sql 已导入过（$menu_count 条），跳过"
fi
run_sql "$ROOT/db/dict.sql"

echo "[2/3] V1.0 业务闭环增量（生成列口径 + 幂等表 + 演示种子）"
# 必须排在 init_tables.sql 之后：这里会把 qty_available 生成列
# 从 qty_on_hand - qty_allocated 改成再减 qty_locked（否则盘点冻结期间超卖）
run_sql "$ROOT/docs/sql/V1.0__wms_inventory_core.sql"
run_sql "$ROOT/docs/sql/V1.0__wms_seed.sql"

if [ "$WITH_LEGACY" = "1" ]; then
  echo "[可选] 导入代码生成器 V5.2 随机演示数据"
  # init_seed.sql 插的是随机数字，且往生成列 qty_available 写值（MySQL 3105），
  # 非幂等；默认跳过，需要时用 --force 容忍单条报错、不影响主流程。
  echo "  注意：该脚本非幂等且会写生成列，已用 --force 容忍错误" >&2
  "$MYSQL" "${MYSQL_ARGS[@]}" --force --database="$DB_NAME" < "$ROOT/db/init_seed.sql" || true
fi

echo "[3/3] 建库体检"
table_count="$("$MYSQL" "${MYSQL_ARGS[@]}" -B -N -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';")"
echo "  表数量：$table_count"
if [ "$table_count" -lt 21 ]; then
  echo "  表数量不足 21，建库失败" >&2
  exit 1
fi

expr="$("$MYSQL" "${MYSQL_ARGS[@]}" -B -N -e \
  "SELECT GENERATION_EXPRESSION FROM information_schema.columns
   WHERE table_schema='$DB_NAME' AND table_name='wms_inventory' AND column_name='qty_available';")"
echo "  qty_available = $expr"
case "$expr" in
  *qty_locked*) echo "  生成列口径正确（含 qty_locked）" ;;
  *) echo "  生成列仍是旧口径，请确认 V1.0__wms_inventory_core.sql 已执行" >&2; exit 1 ;;
esac

log_table="$("$MYSQL" "${MYSQL_ARGS[@]}" -B -N -e \
  "SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema='$DB_NAME' AND table_name='wms_request_log';")"
if [ "$log_table" != "1" ]; then
  echo "  wms_request_log 缺失，幂等端点不可用" >&2
  exit 1
fi

dict_rows="$("$MYSQL" "${MYSQL_ARGS[@]}" --database="$DB_NAME" -B -N -e \
  "SELECT COUNT(*) FROM sys_dict_data;")"
echo "  sys_dict_data 行数：$dict_rows"
echo "建库完成，可以执行：bash scripts/cloud-up.sh --with-web"
