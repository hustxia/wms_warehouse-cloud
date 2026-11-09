#!/usr/bin/env node
/**
 * WMS V1.1 F-08：库存账面 / 流水对账 + 冲销。
 *
 * 对账口径（重要）：
 *   不是「on_hand = Σ 全量流水」——seed 库存自带期初量却没有期初流水，
 *   干净库上按绝对值算会报 3 条假差异。真正的恒等式是增量的：
 *   每条库存记录的 qty_on_hand 必须等于「它最近一条流水的 qty_after」。
 *   一旦有人绕过 InventoryCoreService 直接改库（或跨服务远程提交成功、
 *   本地事务失败造成账面偏移），这个等式就断，本脚本据此报差异。
 *
 * 运行：
 *   node scripts/reconcile.mjs               # 只扫描，有差异退出码 1
 *   node scripts/reconcile.mjs --json        # 机器可读输出
 *   node scripts/reconcile.mjs --fix         # 扫描后调内部冲销端点修正
 *   node scripts/reconcile.mjs --selftest    # 自包含验收：造差异→报出→冲销→归零→清理
 *
 * --fix / --selftest 需要 6 个服务中的 wms-inventory 已启动（默认 8145），
 * 且 APP_INTERNAL_TOKEN 与服务端一致。
 */
import { execFileSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';

const MYSQL_BIN = process.env.MYSQL_BIN || 'C:/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe';
const DB_USER = process.env.DB_USER || 'root';
const DB_PASS = process.env.DB_PASS || 'root';
const DB_NAME = process.env.DB_NAME || 'wms_warehouse';
const INVENTORY_URL = process.env.INVENTORY_URL || 'http://127.0.0.1:8145';
const INTERNAL_TOKEN = process.env.APP_INTERNAL_TOKEN || 'wms-internal';

const JSON_OUT = process.argv.includes('--json');
const FIX = process.argv.includes('--fix');
const SELFTEST = process.argv.includes('--selftest');
const RUN = randomBytes(4).toString('hex');
const SELFTEST_ID = 91002;
const BASE_REF = 'RECONCILE-SELFTEST-BASE';
const FIX_REF = 'RECONCILE-SELFTEST-FIX';

/** 执行 SQL，返回带列名的对象数组 */
function sql(query) {
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

/** 差异清单：跳过没有流水的库存（期初量，无基线可比） */
function scan() {
  return sql(`
    SELECT * FROM (
      SELECT i.id, i.batch_no, i.qty_on_hand AS on_hand,
             (SELECT t.qty_after FROM wms_inventory_txn t
               WHERE t.deleted = 0
                 AND t.warehouse_id = i.warehouse_id
                 AND t.owner_id     = i.owner_id
                 AND t.sku_id       = i.sku_id
                 AND t.location_id  = i.location_id
                 AND t.batch_no     = i.batch_no
               ORDER BY t.id DESC LIMIT 1) AS last_after
      FROM wms_inventory i
      WHERE i.deleted = 0
    ) x
    WHERE last_after IS NOT NULL
      AND ABS(on_hand - last_after) > 0.0005
    ORDER BY id
  `).map((r) => ({
    id: Number(r.id),
    batchNo: r.batch_no,
    onHand: Number(r.on_hand),
    targetOnHand: Number(r.last_after),
    delta: Number(r.on_hand) - Number(r.last_after),
  }));
}

async function adjust(item, requestId, refNo) {
  const res = await fetch(`${INVENTORY_URL}/internal/inventory/reconcile-adjust`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Internal-Token': INTERNAL_TOKEN },
    body: JSON.stringify({
      inventoryId: item.id,
      targetOnHand: item.targetOnHand,
      requestId,
      refNo,
      remark: `对账冲销 ${item.delta > 0 ? '+' : ''}${item.delta}`,
    }),
  });
  const text = await res.text();
  let json = null;
  try { json = JSON.parse(text); } catch { /* 保留原文 */ }
  if (!json || json.code !== 200) {
    throw new Error(`冲销失败 inventoryId=${item.id}：${text.slice(0, 300)}`);
  }
  return json.data;
}

function assert(cond, msg) {
  if (!cond) throw new Error(`断言失败：${msg}`);
}

function printDiffs(diffs) {
  if (!diffs.length) {
    console.log('对账通过：0 条差异（所有有流水的库存，账面 = 最近流水 qty_after）');
    return;
  }
  console.log(`发现 ${diffs.length} 条差异：`);
  console.log('  库存ID  批次              账面      应为      差额');
  for (const d of diffs) {
    console.log(`  ${String(d.id).padEnd(8)}${String(d.batchNo).padEnd(18)}` +
      `${String(d.onHand).padEnd(10)}${String(d.targetOnHand).padEnd(10)}${d.delta}`);
  }
}

/** 自包含验收现场：临时库存 + 一条基线流水 + 一次「绕过核心服务」的直接改库 */
function prepareSelftest() {
  sql(`
    DELETE FROM wms_inventory_txn WHERE ref_order_no IN ('${BASE_REF}', '${FIX_REF}');
    DELETE FROM wms_request_log WHERE request_id LIKE 'reconcile-selftest-%';
    INSERT INTO wms_inventory
      (id, warehouse_id, owner_id, sku_id, location_id, batch_no,
       qty_on_hand, qty_allocated, qty_locked, version, updated_at, create_time, deleted)
    VALUES
      (${SELFTEST_ID}, 1001, 1001, 1001, 1003, 'RECONCILE-SELFTEST',
       50, 0, 0, 0, NOW(), NOW(), 0)
    ON DUPLICATE KEY UPDATE
       qty_on_hand = 50, qty_allocated = 0, qty_locked = 0, deleted = 0, updated_at = NOW();
    INSERT INTO wms_inventory_txn
      (txn_no, txn_type, warehouse_id, owner_id, sku_id, location_id, batch_no,
       qty_change, qty_before, qty_after, ref_order_no, remark, created_at, deleted)
    VALUES
      ('${BASE_REF}', 'ADJUST', 1001, 1001, 1001, 1003, 'RECONCILE-SELFTEST',
       50, 0, 50, '${BASE_REF}', '对账自测基线', NOW(), 0);
    -- 模拟事故：绕过 InventoryCoreService 直接改账面
    UPDATE wms_inventory SET qty_on_hand = 57 WHERE id = ${SELFTEST_ID};
  `);
}

function cleanupSelftest() {
  sql(`
    DELETE FROM wms_inventory_txn WHERE ref_order_no IN ('${BASE_REF}', '${FIX_REF}');
    DELETE FROM wms_request_log WHERE request_id LIKE 'reconcile-selftest-%';
    DELETE FROM wms_inventory WHERE id = ${SELFTEST_ID};
  `);
}

async function main() {
  if (SELFTEST) {
    console.log(`对账冲销自测 | run=${RUN} | inventory=${INVENTORY_URL}\n`);
    cleanupSelftest();
    prepareSelftest();

    let diffs = scan();
    const target = diffs.find((d) => d.id === SELFTEST_ID);
    assert(target, '人为制造的 1 条差异应被对账脚本报出');
    assert(target.onHand === 57 && target.targetOnHand === 50,
      `差异值应为 57 / 50，实际 ${target.onHand} / ${target.targetOnHand}`);
    console.log(`  PASS  造差异后被报出：on_hand=${target.onHand} 应为=${target.targetOnHand}`);

    const requestId = `reconcile-selftest-${RUN}`;
    const first = await adjust(target, requestId, FIX_REF);
    assert(first.replayed === false, '首次冲销 replayed 应为 false');
    assert(first.txnId && typeof first.txnId === 'string', 'txnId 必须字符串化');
    console.log(`  PASS  冲销写入 ADJUST 流水 txnId=${first.txnId} before=${first.before} after=${first.after}`);

    const replay = await adjust(target, requestId, FIX_REF);
    assert(replay.replayed === true, '重复 requestId 应回放上次结果（replayed=true）');
    console.log('  PASS  重复 requestId 回放，未重复写流水');

    diffs = scan();
    assert(!diffs.some((d) => d.id === SELFTEST_ID), '冲销后该库存差异应归零');
    console.log('  PASS  冲销后复查：0 条差异');

    const txnCount = Number(sql(`SELECT COUNT(*) AS c FROM wms_inventory_txn
      WHERE ref_order_no = '${FIX_REF}'`)[0].c);
    assert(txnCount === 1, `ADJUST 流水应恰好 1 条，实际 ${txnCount}`);
    console.log('  PASS  ADJUST 流水恰好 1 条');

    cleanupSelftest();
    console.log('\nSELFTEST PASS：造差异 → 报 1 条 → 冲销（含幂等）→ 0 条差异 → 现场已恢复');
    return;
  }

  let diffs = scan();
  if (JSON_OUT) {
    console.log(JSON.stringify(diffs, null, 2));
  } else {
    printDiffs(diffs);
  }

  if (FIX && diffs.length) {
    for (const d of diffs) {
      const r = await adjust(d, `reconcile-${RUN}-${d.id}`, `RECONCILE-${RUN}`);
      console.log(`  已冲销 inventoryId=${d.id} -> ${r.after}（txnId=${r.txnId}）`);
    }
    diffs = scan();
    if (!JSON_OUT) printDiffs(diffs);
  }

  if (diffs.length) {
    console.error(`对账未通过：仍有 ${diffs.length} 条差异`);
    process.exitCode = 1;
  }
}

try {
  await main();
} catch (e) {
  console.error(`RECONCILE FAILED: ${e.message}`);
  if (SELFTEST) {
    try { cleanupSelftest(); console.error('自测现场已清理'); }
    catch (ce) { console.error(`清理现场失败：${ce.message}`); }
  }
  process.exitCode = 1;
}
