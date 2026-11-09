#!/usr/bin/env node
/**
 * WMS V1.1 S8 UI 层验证：真实打开 4 个业务页，注入登录态，抓 console/网络错误，
 * 并实机检查「业务字段只读」是否真的生效（不是只看源码）。
 *
 * 用法：
 *   TOKEN=<sys_session.token> node scripts/ui-check-pages.mjs
 *   node scripts/ui-check-pages.mjs --token <TOKEN> --app http://127.0.0.1:9040
 *
 * 前置：前端 dev server 9040 已启动；sys_session 里有一条有效 admin 会话；
 *       localStorage.perms 注入 '*:*:*' 让所有权限按钮显示，避免「按钮被权限指令
 *       隐藏」造成只读验收假阳性。
 *
 * 退出码：0 = 4 页全部通过；1 = 有页面不满足；2 = 环境错误。
 */
import { spawn } from 'node:child_process';
import { existsSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { tmpdir } from 'node:os';

const argv = process.argv.slice(2);
const arg = (name, def = null) => {
  const i = argv.indexOf(`--${name}`);
  return i >= 0 && argv[i + 1] && !argv[i + 1].startsWith('--') ? argv[i + 1] : def;
};
const opts = {
  app: arg('app', 'http://127.0.0.1:9040'),
  token: arg('token', process.env.TOKEN || ''),
  outDir: arg('out', resolve('docs/evidence/v1.1')),
  wait: Number(arg('wait', '3200')),
};
if (!opts.token) {
  console.error('缺少 --token（sys_session 里有效的 admin 会话 token）');
  process.exit(2);
}

const BROWSERS = [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
  'C:/Program Files/Microsoft/Edge/Application/msedge.exe',
];
const browser = BROWSERS.find((p) => existsSync(p));
if (!browser) { console.error('未找到 Chrome / Edge'); process.exit(2); }

const PAGES = [
  { name: '入库单', path: '/wms_warehouse/inbound/wms_inbound_order/list', minDisabled: 4 },
  { name: '出库单', path: '/wms_warehouse/outbound/wms_outbound_order/list', minDisabled: 5 },
  { name: '盘点单', path: '/wms_warehouse/stock/wms_stocktake_order/list',
    minDisabled: 4, forbidText: '新增盘点单表' },
  { name: '库存', path: '/wms_warehouse/stock/wms_inventory/list',
    minDisabled: 6, forbidText: '新增库存表', forbidDelete: true },
];

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const profileDir = resolve(tmpdir(), `ui-check-v11-${Date.now()}`);
let child = null;
let ws = null;

function shutdown(code) {
  try { if (child) child.kill(); } catch { /* ignore */ }
  try { if (ws) ws.close(); } catch { /* ignore */ }
  try { rmSync(profileDir, { recursive: true, force: true }); } catch { /* ignore */ }
  process.exit(code);
}

async function freePort() {
  const net = await import('node:net');
  return new Promise((res) => {
    const srv = net.createServer();
    srv.listen(0, '127.0.0.1', () => { const p = srv.address().port; srv.close(() => res(p)); });
  });
}

const consoleErrors = [];
const pageErrors = [];
const failedRequests = [];
const httpErrors = [];

async function main() {
  const debugPort = await freePort();
  child = spawn(browser, [
    '--headless=new', `--remote-debugging-port=${debugPort}`,
    `--user-data-dir=${profileDir}`, '--window-size=1440,1000',
    '--no-first-run', '--no-default-browser-check', '--disable-gpu',
    '--hide-scrollbars', '--disable-extensions', 'about:blank',
  ], { stdio: 'ignore' });

  let ready = false;
  for (let i = 0; i < 60; i++) {
    try { if ((await fetch(`http://127.0.0.1:${debugPort}/json/version`)).ok) { ready = true; break; } }
    catch { /* not up */ }
    await sleep(200);
  }
  if (!ready) { console.error('浏览器调试端口未就绪'); shutdown(2); }

  const target = await (await fetch(
    `http://127.0.0.1:${debugPort}/json/new?${encodeURIComponent('about:blank')}`,
    { method: 'PUT' })).json();
  ws = new WebSocket(target.webSocketDebuggerUrl);
  await new Promise((res, rej) => { ws.onopen = res; ws.onerror = () => rej(new Error('WebSocket 连接失败')); });

  let seq = 0;
  const pending = new Map();
  ws.onmessage = (e) => {
    const msg = JSON.parse(e.data);
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); return; }
    const p = msg.params || {};
    if (msg.method === 'Runtime.consoleAPICalled' && p.type === 'error') {
      consoleErrors.push((p.args || []).map((a) => a.value ?? a.description ?? a.type).join(' ').slice(0, 300));
    } else if (msg.method === 'Runtime.exceptionThrown') {
      pageErrors.push((p.exceptionDetails?.exception?.description || p.exceptionDetails?.text || 'unknown').slice(0, 300));
    } else if (msg.method === 'Network.loadingFailed' && !p.canceled) {
      failedRequests.push(`${p.type || ''} ${p.errorText || ''}`.trim());
    } else if (msg.method === 'Network.responseReceived' && p.response?.status >= 400) {
      httpErrors.push(`${p.response.status} ${p.response.url}`.slice(0, 300));
    }
  };
  const send = (method, params = {}) => new Promise((res, rej) => {
    const id = ++seq;
    pending.set(id, (m) => (m.error ? rej(new Error(`${method}: ${m.error.message}`)) : res(m.result)));
    ws.send(JSON.stringify({ id, method, params }));
  });

  await send('Runtime.enable');
  await send('Network.enable');
  await send('Page.enable');
  await send('Emulation.setDeviceMetricsOverride',
    { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
  // 新文档创建时注入登录态；about:blank 上 localStorage 不可用，必须 try/catch
  await send('Page.addScriptToEvaluateOnNewDocument', {
    source: `try {
      localStorage.setItem('token', ${JSON.stringify(opts.token)});
      localStorage.setItem('username', 'admin');
      localStorage.setItem('perms', JSON.stringify(['*:*:*']));
    } catch (e) { /* about:blank */ }`,
  });

  mkdirSync(opts.outDir, { recursive: true });
  let failures = 0;
  console.log(`UI 实机走查 | app=${opts.app} | ${PAGES.length} 页\n`);

  for (const page of PAGES) {
    const before = {
      console: consoleErrors.length, page: pageErrors.length,
      net: failedRequests.length, http: httpErrors.length,
    };
    const url = opts.app.replace(/\/+$/, '') + '/#' + page.path;
    await send('Page.navigate', { url });
    await sleep(opts.wait);

    // 打开第一个「编辑」弹窗，让 disabled 控件真正渲染进 DOM
    const openResult = await send('Runtime.evaluate', {
      expression: `(() => {
        const btns = [...document.querySelectorAll('button')];
        const edit = btns.find(b => (b.innerText || '').trim() === '编辑');
        if (edit) edit.click();
        return JSON.stringify({ clicked: !!edit, text: document.body.innerText.slice(0, 200) });
      })()`, returnByValue: true,
    });
    await sleep(900);

    const probe = await send('Runtime.evaluate', {
      expression: `(() => {
        const text = document.body.innerText || '';
        const buttons = [...document.querySelectorAll('button')].map(b => (b.innerText || '').trim());
        return JSON.stringify({
          disabledInDialog: document.querySelectorAll('.el-dialog input[disabled]').length,
          hasForbiddenText: ${JSON.stringify(page.forbidText || '')} ? text.includes(${JSON.stringify(page.forbidText || '')}) : false,
          hasDeleteButton: buttons.some(t => t === '删除'),
          rowCount: document.querySelectorAll('.el-table__row').length,
          alert: text.includes('由业务端点写入')
        });
      })()`, returnByValue: true,
    });
    const p = JSON.parse(probe.result.value);
    const errs = {
      console: consoleErrors.slice(before.console),
      page: pageErrors.slice(before.page),
      net: failedRequests.slice(before.net),
      http: httpErrors.slice(before.http),
    };
    const shot = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true });
    const png = resolve(opts.outDir, `${page.name}.png`);
    writeFileSync(png, Buffer.from(shot.data, 'base64'));

    const checks = [
      [p.disabledInDialog >= page.minDisabled, `只读控件 ${p.disabledInDialog} 个（要求 ≥ ${page.minDisabled}）`],
    ];
    if (page.forbidText) checks.push([!p.hasForbiddenText, `不存在「${page.forbidText}」入口`]);
    if (page.forbidDelete) checks.push([!p.hasDeleteButton, '库存页无「删除」按钮']);
    checks.push(
      [errs.console.length === 0, `console error ${errs.console.length} 条`],
      [errs.page.length === 0, `JS 异常 ${errs.page.length} 条`],
      [errs.http.length === 0, `HTTP >= 400 ${errs.http.length} 条`],
      [errs.net.length === 0, `失败请求 ${errs.net.length} 条`],
    );
    const bad = checks.filter(([ok]) => !ok);
    if (bad.length) failures++;
    console.log(`  ${bad.length ? 'FAIL' : 'PASS'}  ${page.name}  表格 ${p.rowCount} 行 / 只读控件 ${p.disabledInDialog} 个 / 截图 ${png}`);
    for (const [, msg] of checks) console.log(`        - ${msg}`);
    for (const e of [...errs.console, ...errs.page, ...errs.http, ...errs.net]) console.log(`        ! ${e}`);
    void openResult;
  }

  console.log(`\n结果：${PAGES.length - failures}/${PAGES.length} 页面通过`);
  shutdown(failures ? 1 : 0);
}

main().catch((e) => { console.error(`UI CHECK FAILED: ${e.message}`); shutdown(2); });
