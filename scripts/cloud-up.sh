#!/usr/bin/env bash
# 启动 WMS cloud 版全套运行环境：Nacos（单机）+ 6 个服务。
#
# 用法（Git Bash，建议在自己开的终端里前台跑，别交给会话后台进程——
#       会话/对话结束时后台进程会被回收，页面立刻全部「请求失败」）：
#   bash scripts/cloud-up.sh --with-web   # 一键：Nacos + 6 服务 + 前端 9040，Ctrl+C 全停
#   bash scripts/cloud-up.sh              # 只起后端（前端另行 npm run dev）
#   bash scripts/cloud-up.sh &            # 后台运行，用 scripts/cloud-down.sh 停止
#
# 两个必须显式指定的参数来自坑库《环境与工具链》N+7 / 《spring-boot-3》第 8 条：
#   1. 宿主会注入 SERVER_PORT（WorkBuddy 桌面端端口），Spring Boot 宽松绑定会拿它
#      覆盖 yml 的 server.port，导致服务抢端口启动失败 → 每个进程单独设 SERVER_PORT + --server.port。
#   2. 多网卡机器注册到 Nacos 的地址可能是虚拟网卡 IP，网关按该地址转发必 503
#      → 显式 --spring.cloud.nacos.discovery.ip=127.0.0.1。
set -euo pipefail

WITH_WEB=0
for arg in "$@"; do
  case "$arg" in
    --with-web) WITH_WEB=1 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

# V1.1 F-09：application.yml / @Value 里已经不带口令与内部令牌的默认值，
# 裸跑 java -jar 会明确报「缺少配置」并退出。这里显式注入**本地开发值**，
# 让一键启动仍然可用；生产部署请用环境变量覆盖，不要沿用下面这两个值。
export DB_PASSWORD="${DB_PASSWORD:-root}"
export APP_INTERNAL_TOKEN="${APP_INTERNAL_TOKEN:-wms-internal}"
echo "凭据注入：DB_PASSWORD / APP_INTERNAL_TOKEN（可用环境变量覆盖）"

# 宿主 JAVA_HOME 常是 Windows 路径（D:\tool\...），统一转 POSIX，避免 -Dloader.path 等参数混用
JAVA_HOME="$(cygpath -u "${JAVA_HOME:-/d/tool/jdk-21.0.12.1+1}")"
NACOS_HOME="${NACOS_HOME:-/d/tool/nacos/nacos}"
LOG_DIR="${LOG_DIR:-/tmp/wms-cloud-logs}"
NACOS_ADDR="${NACOS_ADDR:-127.0.0.1:8848}"
mkdir -p "$LOG_DIR"

echo "JAVA_HOME=$JAVA_HOME"
echo "logs=$LOG_DIR"

# --- Nacos：已在跑就复用 ---
if curl -s --max-time 2 "http://127.0.0.1:8848/nacos/v1/console/health/readiness" | grep -q OK; then
  echo "Nacos 已在运行，复用"
else
  NACOS_WIN="$(cygpath -m "$NACOS_HOME")"
  echo "启动 Nacos（standalone）..."
  SERVER_PORT=8848 "$JAVA_HOME/bin/java" \
    -Xms512m -Xmx512m -Xmn256m \
    -Dnacos.standalone=true \
    -Dloader.path="$NACOS_WIN/plugins,$NACOS_WIN/plugins/health,$NACOS_WIN/plugins/cmdb,$NACOS_WIN/plugins/selector" \
    -Dnacos.home="$NACOS_WIN" \
    -jar "$NACOS_HOME/target/nacos-server.jar" \
    --server.port=8848 \
    --spring.config.additional-location="file:$NACOS_WIN/conf/" \
    --logging.config="$NACOS_WIN/conf/nacos-logback.xml" \
    nacos.nacos -m standalone > "$LOG_DIR/nacos.log" 2>&1 &
  echo $! > "$LOG_DIR/nacos.pid"
  curl -s --retry 45 --retry-delay 2 --retry-connrefused \
    "http://127.0.0.1:8848/nacos/v1/console/health/readiness" > /dev/null
  echo "Nacos ready"
fi

start_service() {
  local mod="$1" port="$2"
  if netstat -ano | grep -q ":$port .*LISTENING"; then
    echo "$mod ($port) 已在运行，跳过"
    return
  fi
  SERVER_PORT="$port" "$JAVA_HOME/bin/java" -jar "$ROOT/$mod/target/$mod.jar" \
    --server.port="$port" \
    --spring.cloud.nacos.discovery.server-addr="$NACOS_ADDR" \
    --spring.cloud.nacos.discovery.ip=127.0.0.1 \
    > "$LOG_DIR/$mod.log" 2>&1 &
  echo $! > "$LOG_DIR/$mod.pid"
  echo "已拉起 $mod ($port) pid=$!"
}

start_service wms-system    8141
start_service wms-master    8142
start_service wms-inbound   8143
start_service wms-outbound  8144
start_service wms-inventory 8145
start_service wms-gateway   8100

echo "等待 6 个服务注册到 Nacos ..."
count=0
for i in $(seq 1 90); do
  # 服务还没注册时 grep -o 无匹配返回 1；pipefail + set -e 会把整个脚本杀掉
  # （连带宿主回收所有子进程 → 页面全部「请求失败」）。这里显式吞掉非 0 退出码。
  count=$( { curl -s --max-time 3 "http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20" \
    | grep -o 'wms-[a-z]*' | sort -u | wc -l; } || true )
  count=${count:-0}
  # 不能用 `[ ... ] && break`：条件为假时整个列表返回 1，set -e 同样会退出
  if [ "$count" -ge 6 ]; then
    break
  fi
  sleep 1
done
echo "注册服务数：$count"

if [ "$WITH_WEB" = "1" ]; then
  WEB_DIR="${WEB_DIR:-$ROOT/../wms_warehouse-web}"
  if netstat -ano | grep -q ":9040 .*LISTENING"; then
    echo "前端 dev server (9040) 已在运行，跳过"
  else
    echo "启动前端 dev server (9040) ..."
    # 坑库《环境与工具链》N+3：宿主注入的 NODE_OPTIONS shim 会让 npm 构建失败，必须清空
    ( cd "$WEB_DIR" && NODE_OPTIONS= npm run dev ) > "$LOG_DIR/web.log" 2>&1 &
    echo $! > "$LOG_DIR/web.pid"
    echo "前端已拉起，日志：$LOG_DIR/web.log"
  fi
fi

echo "启动完成。可执行：node scripts/e2e-cloud.mjs"
echo "页面：http://127.0.0.1:9040  网关：http://127.0.0.1:8100"
echo "（本脚本保持前台以托管子进程；停止用 scripts/cloud-down.sh --with-nacos）"
wait
