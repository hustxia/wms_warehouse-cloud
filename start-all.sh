#!/bin/bash
# WMS 微服务一键启动（先决条件：MySQL 已启动、Nacos 已在 8848 运行）
# 用法：./start-all.sh
set -e
cd "$(dirname "$0")"
JARS=(wms-system/target/wms-system.jar wms-master/target/wms-master.jar wms-inbound/target/wms-inbound.jar wms-outbound/target/wms-outbound.jar wms-inventory/target/wms-inventory.jar wms-gateway/target/wms-gateway.jar)
mkdir -p logs
for jar in "${JARS[@]}"; do
  name=$(basename "$jar" .jar)
  echo "启动 $name ..."
  nohup java -jar "$jar" > "logs/$name.log" 2>&1 &
  sleep 2
done
echo "全部已拉起，端口：system=8141 master=8142 inbound=8143 outbound=8144 inventory=8145 gateway=8100"
echo "查看日志：logs/<服务名>.log"
