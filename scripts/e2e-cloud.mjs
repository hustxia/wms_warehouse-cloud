#!/usr/bin/env node
/**
 * WMS V1.0 微服务版端到端闭环验证（S8）。
 *
 * 前置：Nacos 8848 + wms-system/master/inbound/outbound/inventory/gateway 六个服务已启动
 *       （见 scripts/cloud-up.sh）。数据库 wms_warehouse 已灌 V1.0 seed。
 *
 * 运行：node scripts/e2e-cloud.mjs            # 跑完自动恢复 seed 现场
 *       node scripts/e2e-cloud.mjs --keep     # 保留结果，便于人工查库
 *
 * 设计要点：
 * 1. 只用 V1.0 seed 的 1001 号入库单/出库单与 3 行库存，开始前把这几行重置回初始值，
 *    其余演示数据一律不碰；因此脚本可反复运行，结果稳定。
 * 2. 登录接口的验证码是图形算术题，自动化不可解，所以按集成测试同构方式直接向
 *    sys_session 插一条 admin 会话；AuthContextFilter / PermissionChecker 全链路仍是真实的。
 * 3. 所有断言走 HTTP（网关）+ 数据库双向核对，不靠「接口返回 200」就算通过。
 */
import { execFileSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { writeFileSync } from 'node:fs';

const GATEWAY = process.env.GATEWAY_URL || 'http://127.0.0.1:8100';
const INVENTORY_DIRECT = process.env.INVENTORY_URL || 'http://127.0.0.1:8145';
const MYSQL_BIN = process.env.MYSQL_BIN || 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';
const DB_USER = process.env.DB_USER || 'root';
const DB_PASS = process.env.DB_PASS || 'root';
const DB_NAME = process.env.DB_NAME || 'wms_warehouse';
// V1.1 F-09：内部端点不再带默认令牌，必须由环境注入；脚本与 cloud-up.sh 保持同源
const INTERNAL_TOKEN = process.env.APP_INTERNAL_TOKEN || 'wms-internal';
// V1.1 F-02：verify.sh 会把报告写进前端 public/，库存页据此展示并发验收结果
const REPORT_FILE = process.env.E2E_REPORT_FILE || '';
const KEEP = process.argv.includes('--keep');

const RUN = randomBytes(4).toString('hex');
const RID = (action) => `e2e-${RUN}-${action}`;
const TOKEN = `e2e-${RUN}-token`;
const IN_NO = 'IB-DEMO-0001';
const OUT_NO = 'OB-DEMO-0001';

let checks = 0;
let failures = 0;
let currentStep = '';

function assert(cond, msg) {
  checks++;
  if (!cond) {
    failures++;
    throw new Error(`[${currentStep}] 断言失败：${msg}`);
  }
}
function eq(actual, expected, msg) {
  assert(Math.abs(Number(actual) - Number(expected)) < 1e-6,
    `${msg}（期望 ${expected}，实际 ${actual}）`);
}
function ok(cond, msg) {
  assert(!!cond, msg);
}

/** 执行 SQL，返回按列位置切分的行数组（-B -N --raw，无表头） */
function sql(query) {
  const out = execFileSync(MYSQL_BIN,
    [`-u${DB_USER}`, `-p${DB_PASS}`, '-B', '-N', '--raw', '--default-character-set=utf8mb4',
      '-e', query, DB_NAME],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'], maxBuffer: 16 * 1024 * 1024 });
  // Windows mysql.exe 的 batch 输出是 CRLF，最后一列会带上 \r，必须剥掉
  return out.split('\n').map((l) => l.replace(/\r$/, '')).filter(Boolean)
    .map((line) => line.split('\t'));
}
function sqlOne(query) {
  const rows = sql(query);
  return rows.length ? rows[0][0] : null;
}
/** 取带列名的对象数组 */
function sqlObjects(query) {
  const out = execFileSync(MYSQL_BIN,
    [`-u${DB_USER}`, `-p${DB_PASS}`, '-B', '--raw', '--default-character-set=utf8mb4',
      '-e', query, DB_NAME],
    { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'], maxBuffer: 16 * 1024 * 1024 });
  const lines = out.split('\n').map((l) => l.replace(/\r$/, '')).filter(Boolean);
  if (!lines.length) return [];
  const cols = lines[0].split('\t');
  return lines.slice(1).map((line) => {
    const vals = line.split('\t');
    return Object.fromEntries(cols.map((c, i) => [c, vals[i]]));
  });
}

const HEADERS = { 'Content-Type': 'application/json', Authorization: `Bearer ${TOKEN}` };

async function raw(method, url, body, headers = HEADERS) {
  const res = await fetch(url, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* 非 JSON 保留原文 */ }
  return { status: res.status, json, text };
}
async function api(method, path, body, label) {
  const r = await raw(method, GATEWAY + path, body);
  assert(r.status === 200, `${label} HTTP ${r.status}：${r.text.slice(0, 300)}`);
  assert(r.json && r.json.code === 200,
    `${label} 业务码非 200：${r.text.slice(0, 300)}`);
  return r.json.data;
}
function inventoryRow(id) {
  return sqlObjects(`SELECT id, warehouse_id, owner_id, sku_id, location_id, batch_no,
      qty_on_hand, qty_allocated, qty_locked, qty_available
      FROM wms_inventory WHERE id = ${id}`)[0];
}
function inventoryByBatch(batchNo) {
  return sqlObjects(`SELECT id, qty_on_hand, qty_allocated, qty_locked, qty_available
      FROM wms_inventory WHERE sku_id = 1001 AND batch_no = '${batchNo}' AND deleted = 0`)[0];
}

/** 把 e2e 涉及的几行恢复到 V1.0 seed 初始状态；只删本脚本产生的记录 */
function reset(withSession = true) {
  sql(`
    DELETE FROM wms_inventory_txn
      WHERE ref_order_no IN ('${IN_NO}', '${OUT_NO}')
         OR ref_order_no IN (SELECT stocktake_no FROM wms_stocktake_order WHERE stocktake_no LIKE 'ST%')
         OR ref_order_no LIKE 'CONCURRENT-%'
         OR ref_order_no LIKE 'RECONCILE-%';
    DELETE FROM wms_inventory WHERE id = 91001;
    DELETE FROM wms_request_log WHERE request_id LIKE 'e2e-%' OR request_id LIKE 'reconcile-%';
    DELETE FROM wms_allocation WHERE order_id = 1001;
    DELETE FROM wms_pick_task WHERE order_id = 1001;
    DELETE FROM wms_shipment WHERE order_id = 1001;
    DELETE FROM wms_receipt_record WHERE order_id = 1001;
    DELETE FROM wms_putaway_task WHERE order_id = 1001;
    DELETE FROM wms_inventory_lock WHERE ref_order_no LIKE 'ST%';
    DELETE FROM wms_stocktake_item WHERE stocktake_id IN
      (SELECT id FROM wms_stocktake_order WHERE stocktake_no LIKE 'ST%');
    DELETE FROM wms_stocktake_order WHERE stocktake_no LIKE 'ST%';
    DELETE FROM wms_inventory WHERE id > 1003 AND sku_id = 1001
      AND batch_no IN ('B20280101', 'B20280201');
    UPDATE wms_inventory SET qty_on_hand = 6,  qty_allocated = 0, qty_locked = 0 WHERE id = 1001;
    UPDATE wms_inventory SET qty_on_hand = 4,  qty_allocated = 0, qty_locked = 0 WHERE id = 1002;
    UPDATE wms_inventory SET qty_on_hand = 50, qty_allocated = 0, qty_locked = 0 WHERE id = 1003;
    UPDATE wms_inbound_order SET status = 'CREATED' WHERE id = 1001;
    UPDATE wms_inbound_order_item SET received_qty = 0, putaway_qty = 0, status = 'PENDING'
      WHERE order_id = 1001;
    UPDATE wms_outbound_order SET status = 'CREATED', carrier = NULL, tracking_no = NULL
      WHERE id = 1001;
    UPDATE wms_outbound_order_item SET allocated_qty = 0, picked_qty = 0, shipped_qty = 0,
      status = 'PENDING' WHERE order_id = 1001;
    DELETE FROM sys_session WHERE token LIKE 'e2e-%';
  `);
  if (withSession) {
    sql(`INSERT INTO sys_session (token, username, login_ip, expire_time)
      VALUES ('${TOKEN}', 'admin', '127.0.0.1', DATE_ADD(NOW(), INTERVAL 2 HOUR))`);
  }
}

async function step(name, fn) {
  currentStep = name;
  const t0 = Date.now();
  try {
    const ret = await fn();
    console.log(`  PASS  ${name}  (${Date.now() - t0}ms)`);
    return ret;
  } catch (e) {
    console.log(`  FAIL  ${name}  -> ${e.message}`);
    throw e;
  }
}

const keyOf = (r) => [r.warehouse_id, r.owner_id, r.sku_id, r.location_id, r.batch_no].join('|');

async function main() {
  console.log(`WMS cloud e2e | run=${RUN} | gateway=${GATEWAY} | keep=${KEEP}\n`);
  reset();
  const baseline = sqlObjects(`SELECT id, warehouse_id, owner_id, sku_id, location_id, batch_no,
      qty_on_hand, qty_allocated, qty_locked, qty_available FROM wms_inventory WHERE deleted = 0`);
  const beforeByKey = new Map(baseline.map((r) => [keyOf(r), Number(r.qty_on_hand)]));
  const maxTxnId = Number(sqlOne('SELECT COALESCE(MAX(id), 0) FROM wms_inventory_txn'));

  console.log('[网关与鉴权边界]');
  await step('网关路由 + admin 会话可用', async () => {
    // /page 生成的是 POST，GET /{id} 才是列表页的详情端点
    const r = await raw('GET', `${GATEWAY}/wms_inbound_order/1001`);
    assert(r.status === 200, `HTTP ${r.status}：${r.text.slice(0, 300)}`);
    assert(r.json && r.json.code === 200, `业务码 ${r.json && r.json.code}：${r.text.slice(0, 300)}`);
  });
  await step('无 token 访问业务接口被拒 401', async () => {
    const r = await raw('GET', `${GATEWAY}/wms_inbound_order/1001`,
      undefined, { 'Content-Type': 'application/json' });
    assert(r.status === 401, `HTTP ${r.status}：${r.text.slice(0, 200)}`);
  });
  await step('网关不路由 /internal/**（404）', async () => {
    const r = await raw('POST', `${GATEWAY}/internal/inventory/fefo-candidates`,
      { warehouseId: 1001, ownerId: 1001, skuId: 1001 });
    assert(r.status === 404, `HTTP ${r.status}`);
  });
  await step('直连内部接口缺令牌被拒 403', async () => {
    const r = await raw('POST', `${INVENTORY_DIRECT}/internal/inventory/fefo-candidates`,
      { warehouseId: 1001, ownerId: 1001, skuId: 1001 },
      { 'Content-Type': 'application/json' });
    assert(r.json && r.json.code === 403, `返回 ${r.text.slice(0, 200)}`);
  });

  console.log('\n[入库闭环：收货 → 上架]');
  const receiveRid = RID('receive');
  await step('收货登记 3 行全量 → RECEIVED', async () => {
    const d = await api('POST', '/wms_inbound_order/1001/receive', {
      requestId: receiveRid,
      items: [
        { itemId: 1001, receivedQty: 20 },
        { itemId: 1002, receivedQty: 10 },
        { itemId: 1003, receivedQty: 30 },
      ],
    }, '收货');
    eq(d.status === 'RECEIVED' ? 1 : 0, 1, '单据状态应为 RECEIVED');
    assert(typeof d.items[0].itemId === 'string',
      'itemId 必须以字符串下发，否则前端拿雪花 ID 会丢精度');
    eq(sqlOne('SELECT SUM(received_qty) FROM wms_inbound_order_item WHERE order_id = 1001'), 60,
      '实收总量');
  });
  await step('重复提交同一 requestId → 原样回放且不重复累加', async () => {
    const d = await api('POST', '/wms_inbound_order/1001/receive', {
      requestId: receiveRid,
      items: [{ itemId: 1001, receivedQty: 20 }],
    }, '重复收货');
    eq(d.replayed === true ? 1 : 0, 1, '应标记 replayed=true');
    eq(sqlOne('SELECT SUM(received_qty) FROM wms_inbound_order_item WHERE order_id = 1001'), 60,
      '实收总量未被重复累加');
  });
  await step('未收货直接上架被状态机拦截', async () => {
    // 用另一张仍是 CREATED 的单据（seed 1002）验证越级；该单无明细，端点会先校验状态
    const r = await raw('POST', `${GATEWAY}/wms_inbound_order/2/putaway`,
      { requestId: RID('illegal-putaway'), items: [{ itemId: 999999, locationId: 1001, putawayQty: 1 }] });
    assert(r.json && r.json.code !== 200, `不该放行：${r.text.slice(0, 200)}`);
  });
  await step('上架 → 库存新增 + PUTAWAY_DONE', async () => {
    const d = await api('POST', '/wms_inbound_order/1001/putaway', {
      requestId: RID('putaway'),
      items: [
        { itemId: 1001, locationId: 1001, putawayQty: 20 },
        { itemId: 1002, locationId: 1001, putawayQty: 10 },
        { itemId: 1003, locationId: 1002, putawayQty: 30 },
      ],
    }, '上架');
    eq(d.status === 'PUTAWAY_DONE' ? 1 : 0, 1, '单据状态应为 PUTAWAY_DONE');
    eq(d.putawayQty, 60, '上架总量');
    eq(inventoryByBatch('B20280101').qty_on_hand, 20, '批次 B20280101 在库量');
    eq(inventoryByBatch('B20280201').qty_on_hand, 10, '批次 B20280201 在库量');
    eq(inventoryRow(1003).qty_on_hand, 80, 'SKU1002 在库量 50+30');
  });

  console.log('\n[出库闭环：FEFO 分配 → 取消 → 重分配 → 拣货 → 发运]');
  let allocSlices = [];
  const allocBefore = new Map();
  await step('分配 8 件：严格按效期正序（B20261201 6 件 + 次近效期批次 2 件）', async () => {
    // 期望顺序直接取 SQL 的 FEFO 排序结果，不用硬编码批次——上架新批次的效期是按保质期推算的
    const cand = sqlObjects(`SELECT id, batch_no, expiry_date, qty_available FROM wms_inventory
      WHERE deleted = 0 AND warehouse_id = 1001 AND owner_id = 1001 AND sku_id = 1001
        AND qty_on_hand - qty_allocated - qty_locked > 0
      ORDER BY CASE WHEN expiry_date IS NULL THEN 1 ELSE 0 END,
        expiry_date ASC, batch_no ASC, location_id ASC`);
    const d = await api('POST', '/wms_outbound_order/1001/allocate',
      { requestId: RID('allocate-1') }, '分配');
    eq(d.status === 'ALLOCATED' ? 1 : 0, 1, '单据状态应为 ALLOCATED');
    const slices = d.items[0].allocations;
    allocSlices = slices;
    eq(slices.length, 2, '分配切片数');
    eq(slices[0].batchNo === cand[0].batch_no ? 1 : 0, 1,
      `首个切片应为效期最近的 ${cand[0].batch_no}，实际 ${slices[0].batchNo}`);
    eq(slices[0].qty, 6, '首个切片数量');
    eq(slices[1].batchNo === cand[1].batch_no ? 1 : 0, 1,
      `第二个切片应为 ${cand[1].batch_no}，实际 ${slices[1].batchNo}`);
    eq(slices[1].qty, 2, '第二个切片数量');
    assert(String(cand[0].expiry_date) < String(cand[1].expiry_date),
      `FEFO 前提：${cand[0].expiry_date} 应早于 ${cand[1].expiry_date}`);
    for (const s of slices) {
      const inv = inventoryRow(s.inventoryId);
      allocBefore.set(String(s.inventoryId), Number(inv.qty_on_hand));
      eq(inv.qty_allocated, s.qty, `库存 ${s.inventoryId} 占用量`);
      eq(inv.qty_available, Number(inv.qty_on_hand) - Number(s.qty),
        `库存 ${s.inventoryId} 可用量生成列`);
    }
    console.log(`        FEFO 顺序：${slices.map((s) => `${s.batchNo}(${s.qty})`).join(' -> ')}`);
  });
  await step('取消分配：占用全部释放，单据回 CREATED', async () => {
    const d = await api('POST', '/wms_outbound_order/1001/cancel-allocate',
      { requestId: RID('cancel-allocate') }, '取消分配');
    eq(d.status === 'CREATED' ? 1 : 0, 1, '单据状态应为 CREATED');
    for (const s of allocSlices) {
      eq(inventoryRow(s.inventoryId).qty_allocated, 0, `库存 ${s.inventoryId} 占用释放`);
      eq(inventoryRow(s.inventoryId).qty_available, allocBefore.get(String(s.inventoryId)),
        `库存 ${s.inventoryId} 可用量恢复`);
    }
  });
  await step('重新分配 → ALLOCATED', async () => {
    const d = await api('POST', '/wms_outbound_order/1001/allocate',
      { requestId: RID('allocate-2') }, '重新分配');
    eq(d.status === 'ALLOCATED' ? 1 : 0, 1, '单据状态应为 ALLOCATED');
  });
  await step('拣货 8 件：按分配切片扣在库量并销占用 → PICKED', async () => {
    const d = await api('POST', '/wms_outbound_order/1001/pick', {
      requestId: RID('pick'),
      items: [{ itemId: 1001, pickedQty: 8 }],
    }, '拣货');
    eq(d.status === 'PICKED' ? 1 : 0, 1, '单据状态应为 PICKED');
    eq(d.pickedQty, 8, '拣货数量');
    for (const s of allocSlices) {
      const inv = inventoryRow(s.inventoryId);
      eq(inv.qty_on_hand, allocBefore.get(String(s.inventoryId)) - Number(s.qty),
        `库存 ${s.inventoryId} 在库量扣减`);
      eq(inv.qty_allocated, 0, `库存 ${s.inventoryId} 占用销账`);
    }
    eq(inventoryRow(1002).qty_on_hand, 4, '未被 FEFO 选中的最远效期批次保持 4');
  });
  const shipRid = RID('ship');
  await step('发运 → SHIPPED 且生成发货单', async () => {
    const d = await api('POST', '/wms_outbound_order/1001/ship',
      { requestId: shipRid, carrier: '顺丰', trackingNo: `SF-${RUN}` }, '发运');
    eq(d.status === 'SHIPPED' ? 1 : 0, 1, '单据状态应为 SHIPPED');
    ok(String(d.shipmentNo).startsWith('SH'), '发货单号应以 SH 开头');
    eq(sqlOne(`SELECT COUNT(*) FROM wms_shipment WHERE order_id = 1001`), 1, '发货单条数');
  });
  await step('发运重复提交回放，不产生第二张发货单', async () => {
    const d = await api('POST', '/wms_outbound_order/1001/ship',
      { requestId: shipRid, carrier: '顺丰', trackingNo: `SF-${RUN}` }, '重复发运');
    eq(d.replayed === true ? 1 : 0, 1, '应标记 replayed=true');
    eq(sqlOne('SELECT COUNT(*) FROM wms_shipment WHERE order_id = 1001'), 1, '发货单条数');
  });

  console.log('\n[盘点闭环：快照 → 冻结 → 录实盘 → 确认调整]');
  let stocktakeId, stocktakeItemId;
  await step('按库位 1002 建盘点单，快照 1 行', async () => {
    const d = await api('POST', '/wms_stocktake_order/create', {
      requestId: RID('stocktake-create'),
      warehouseId: 1001, locationId: 1002, stocktakeType: '抽盘',
    }, '创建盘点单');
    assert(typeof d.stocktakeId === 'string',
      'stocktakeId 必须以字符串下发，否则前端拿雪花 ID 会丢精度');
    stocktakeId = d.stocktakeId;
    eq(d.itemCount, 1, '快照明细数');
    const rows = sqlObjects(`SELECT id, system_qty FROM wms_stocktake_item
      WHERE stocktake_id = ${stocktakeId}`);
    eq(rows.length, 1, '明细行数');
    eq(rows[0].system_qty, 80, '快照应锁定当前账面 80');
    stocktakeItemId = rows[0].id;
  });
  await step('冻结：可用量归零，锁定量 80', async () => {
    const d = await api('POST', `/wms_stocktake_order/${stocktakeId}/freeze`,
      { requestId: RID('stocktake-freeze') }, '冻结');
    eq(d.status === 'FROZEN' ? 1 : 0, 1, '单据状态应为 FROZEN');
    eq(d.lockedCount, 1, '冻结行数');
    eq(inventoryRow(1003).qty_locked, 80, '锁定量');
    eq(inventoryRow(1003).qty_available, 0, '可用量（生成列）');
  });
  await step('录实盘 78 → 差异 -2（生成列 diff_qty 由 DB 计算）', async () => {
    const d = await api('POST', `/wms_stocktake_item/${stocktakeItemId}/count`,
      { requestId: RID('stocktake-count'), countedQty: 78 }, '录实盘');
    eq(d.diffQty, -2, '接口返回差异');
    eq(sqlOne(`SELECT diff_qty FROM wms_stocktake_item WHERE id = ${stocktakeItemId}`), -2,
      '数据库生成列差异');
  });
  await step('确认调整：先解冻再盘亏 → 在库 78、锁定 0、状态 CONFIRMED', async () => {
    const d = await api('POST', `/wms_stocktake_order/${stocktakeId}/confirm`,
      { requestId: RID('stocktake-confirm') }, '确认调整');
    eq(d.status === 'CONFIRMED' ? 1 : 0, 1, '单据状态应为 CONFIRMED');
    eq(d.adjustedCount, 1, '调整行数');
    eq(d.diffTotal, -2, '差异合计');
    const inv = inventoryRow(1003);
    eq(inv.qty_on_hand, 78, '盘亏后账面');
    eq(inv.qty_locked, 0, '冻结已释放');
    eq(inv.qty_available, 78, '可用量恢复');
  });

  console.log('\n[全局一致性：流水恒等式 + 生成列]');
  await step('每条流水 qty_after - qty_before = qty_change', async () => {
    const txns = sqlObjects(`SELECT id, qty_change, qty_before, qty_after
      FROM wms_inventory_txn WHERE id > ${maxTxnId} ORDER BY id`);
    ok(txns.length >= 10, `流水条数应 >= 10，实际 ${txns.length}`);
    for (const t of txns) {
      eq(Number(t.qty_after) - Number(t.qty_before), Number(t.qty_change),
        `流水 ${t.id} 前后台量差`);
    }
    console.log(`        本次产生 ${txns.length} 条库存流水`);
  });
  await step('按业务键：库存变化量 = 流水 qty_change 之和', async () => {
    const txns = sqlObjects(`SELECT warehouse_id, owner_id, sku_id, location_id, batch_no, qty_change
      FROM wms_inventory_txn WHERE id > ${maxTxnId}`);
    const sums = new Map();
    for (const t of txns) {
      const k = keyOf(t);
      sums.set(k, (sums.get(k) || 0) + Number(t.qty_change));
    }
    const after = sqlObjects(`SELECT warehouse_id, owner_id, sku_id, location_id, batch_no,
      qty_on_hand, qty_allocated, qty_locked, qty_available
      FROM wms_inventory WHERE deleted = 0`);
    for (const r of after) {
      const k = keyOf(r);
      const delta = Number(r.qty_on_hand) - (beforeByKey.get(k) || 0);
      eq(delta, sums.get(k) || 0, `业务键 ${k} 的账面变化与流水合计`);
      eq(Number(r.qty_available),
        Number(r.qty_on_hand) - Number(r.qty_allocated) - Number(r.qty_locked),
        `业务键 ${k} 的 qty_available 生成列`);
    }
    ok(after.length >= baseline.length, '库存行数不应减少');
  });
  await step('单据终态：入库 PUTAWAY_DONE / 出库 SHIPPED / 盘点 CONFIRMED', async () => {
    eq(sqlOne("SELECT COUNT(*) FROM wms_inbound_order WHERE id = 1001 AND status = 'PUTAWAY_DONE'"),
      1, '入库单终态');
    eq(sqlOne("SELECT COUNT(*) FROM wms_outbound_order WHERE id = 1001 AND status = 'SHIPPED'"),
      1, '出库单终态');
    eq(sqlOne(`SELECT COUNT(*) FROM wms_stocktake_order WHERE id = ${stocktakeId} AND status = 'CONFIRMED'`),
      1, '盘点单终态');
  });

  // ==================================================================
  // V1.1 F-05：并发验收。放在全局一致性检查之后，避免临时库存/流水污染
  // 「按业务键：库存变化量 = 流水 qty_change 之和」的 beforeByKey 基线。
  // ==================================================================
  console.log('\n[并发验收：条件 UPDATE 防超卖]');
  const CONCURRENT_ID = 91001;
  const CONCURRENT_BATCH = 'CONCURRENT-TEST';
  const INTERNAL_HEADERS = { 'Content-Type': 'application/json', 'X-Internal-Token': INTERNAL_TOKEN };
  const concurrent = { scenario100: null, scenario150: null };

  function prepareConcurrentStock(onHand) {
    sql(`
      DELETE FROM wms_inventory_txn WHERE ref_order_no LIKE 'CONCURRENT-%';
      INSERT INTO wms_inventory
        (id, warehouse_id, owner_id, sku_id, location_id, batch_no,
         qty_on_hand, qty_allocated, qty_locked, version, updated_at, create_time, deleted)
      VALUES
        (${CONCURRENT_ID}, 1001, 1001, 1001, 1002, '${CONCURRENT_BATCH}',
         ${onHand}, 0, 0, 0, NOW(), NOW(), 0)
      ON DUPLICATE KEY UPDATE
         qty_on_hand = ${onHand}, qty_allocated = 0, qty_locked = 0,
         deleted = 0, updated_at = NOW();
    `);
  }

  const decreaseOnce = (n) => raw('POST', `${INVENTORY_DIRECT}/internal/inventory/decrease`, {
    inventoryId: CONCURRENT_ID,
    qty: 1,
    txnType: 'PICK',
    refNo: `CONCURRENT-${RUN}-${n}`,
    remark: `并发验收 #${n}`,
  }, INTERNAL_HEADERS);

  const concurrentSummary = (results) => {
    const succeeded = results.filter((r) => r.json && r.json.code === 200);
    const rejected = results.filter((r) => !r.json || r.json.code !== 200);
    return { succeeded, rejected };
  };

  const assertConcurrentFinalState = (expectedTxns) => {
    const row = sqlObjects(`SELECT qty_on_hand, qty_available
      FROM wms_inventory WHERE id = ${CONCURRENT_ID}`)[0];
    eq(Number(row.qty_on_hand), 0, '并发结束后在库量');
    ok(Number(row.qty_on_hand) >= 0, '在库量不得为负（超卖）');
    eq(Number(row.qty_available), 0, '并发结束后可用量');
    eq(Number(sqlOne(`SELECT COUNT(*) FROM wms_inventory_txn
      WHERE ref_order_no LIKE 'CONCURRENT-%'`)), expectedTxns, '流水条数');
    eq(Number(sqlOne(`SELECT COALESCE(SUM(qty_change), 0) FROM wms_inventory_txn
      WHERE ref_order_no LIKE 'CONCURRENT-%'`)), -expectedTxns, '流水合计');
    eq(Number(sqlOne(`SELECT COUNT(*) FROM wms_inventory_txn
      WHERE ref_order_no LIKE 'CONCURRENT-%' AND qty_after - qty_before <> qty_change`)),
      0, '每条流水前后台量差等于 qty_change');
  };

  await step('100 并发各扣 1 件（初始 100）→ 全部成功 / 账面归零 / 流水 100 条', async () => {
    prepareConcurrentStock(100);
    const results = await Promise.all(Array.from({ length: 100 }, (_, i) => decreaseOnce(i)));
    const { succeeded, rejected } = concurrentSummary(results);
    eq(succeeded.length, 100,
      `成功次数（首个拒绝响应：${rejected.length ? rejected[0].text.slice(0, 160) : '无'}）`);
    assertConcurrentFinalState(100);
    concurrent.scenario100 = { requests: 100, succeeded: 100, rejected: 0, txns: 100, negative: false };
    console.log(`        成功 100 / 拒绝 0 / 流水 100 / 无负数`);
  });

  await step('150 并发各扣 1 件（初始 100）→ 成功 100 / 拒绝 50 且均为库存不足 / 无超卖', async () => {
    prepareConcurrentStock(100);
    const results = await Promise.all(Array.from({ length: 150 }, (_, i) => decreaseOnce(i)));
    const { succeeded, rejected } = concurrentSummary(results);
    eq(succeeded.length, 100, '成功次数');
    eq(rejected.length, 50, '拒绝次数');
    ok(rejected.every((r) => r.json && String(r.json.msg || '').includes('库存不足')),
      '被拒请求必须都是「库存不足」，不能是死锁/连接超时/500');
    assertConcurrentFinalState(100);
    concurrent.scenario150 = { requests: 150, succeeded: 100, rejected: 50, txns: 100, negative: false };
    console.log(`        成功 100 / 拒绝 50 / 流水 100 / 无负数`);
  });

  if (REPORT_FILE) {
    writeFileSync(REPORT_FILE, JSON.stringify({
      run: RUN,
      generatedAt: new Date().toISOString(),
      checks,
      failures,
      passed: failures === 0,
      concurrent,
    }, null, 2), 'utf8');
    console.log(`验收报告已写入 ${REPORT_FILE}`);
  }

  console.log(`\n结果：${checks - failures}/${checks} 断言通过`);
  if (failures) throw new Error(`${failures} 条断言失败`);
}

try {
  await main();
} catch (e) {
  console.error(`\nE2E FAILED: ${e.message}`);
  process.exitCode = 1;
} finally {
  if (!KEEP) {
    try { reset(false); console.log('现场已恢复（seed 数据 + 清除 e2e 写入）'); }
    catch (e) { console.error(`恢复现场失败：${e.message}`); }
  } else {
    console.log('--keep：保留本次结果，下次运行会自动重置');
  }
}
