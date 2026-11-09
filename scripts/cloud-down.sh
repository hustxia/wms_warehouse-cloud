#!/usr/bin/env bash
# 停止 WMS cloud 版 6 个服务（按端口找 PID，Windows/Git Bash 用 taskkill）。
#
#   bash scripts/cloud-down.sh                        # 停 6 个服务，保留 Nacos 与前端
#   bash scripts/cloud-down.sh --with-nacos            # 连 Nacos 一起停
#   bash scripts/cloud-down.sh --with-web             # 连前端 dev server 一起停
#   bash scripts/cloud-down.sh --with-nacos --with-web # 全停
#   bash scripts/cloud-down.sh --only 8143 8144 8145  # 只停指定端口
set -uo pipefail

PORTS=(8100 8141 8142 8143 8144 8145)
EXTRA=()
if [ "${1:-}" = "--only" ]; then
  shift
  PORTS=("$@")
else
  for arg in "$@"; do
    case "$arg" in
      --with-nacos) EXTRA+=(8848) ;;
      --with-web) EXTRA+=(9040) ;;
    esac
  done
  PORTS=("${EXTRA[@]}" "${PORTS[@]}")
fi

for port in "${PORTS[@]}"; do
  pid=$(netstat -ano | grep ":$port " | grep LISTENING | awk '{print $5}' | head -1)
  if [ -n "$pid" ]; then
    echo "stop $port (pid $pid)"
    taskkill //PID "$pid" //F > /dev/null 2>&1
  else
    echo "port $port 未监听"
  fi
done
