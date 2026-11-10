import request from '@/utils/request'
import { getWmsLocationList } from '@/api/wms_location'

/**
 * WMS V1.0 业务闭环接口（S7）。
 *
 * 全部走网关 8100，前端 /api/demo 前缀由 vue.config.js 的 pathRewrite 去掉。
 * 每个写操作都必须带 requestId：后端 wms_request_log 用它做幂等占位，
 * 重复提交会原样返回上次结果，不会重复加减库存。
 */
const BASE = '/api/demo'

export function newRequestId(action) {
  return `web-${action}-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

const post = (url, data) => request({ url: BASE + url, method: 'post', data })
const get = (url) => request({ url: BASE + url, method: 'get' })
/** 内部端点返回 Result<List<T>>，拦截器已把 code!=200 变成 reject，这里只取 data */
const listOf = async (url) => {
  const res = await get(url)
  return (res && res.data) || []
}

// ===== 入库闭环 =====
export const receiveInbound = (id, data) => post(`/wms_inbound_order/${id}/receive`, data)
export const putawayInbound = (id, data) => post(`/wms_inbound_order/${id}/putaway`, data)

// ===== 出库闭环 =====
export const allocateOutbound = (id, data) => post(`/wms_outbound_order/${id}/allocate`, data)
export const cancelAllocateOutbound = (id, data) => post(`/wms_outbound_order/${id}/cancel-allocate`, data)
export const pickOutbound = (id, data) => post(`/wms_outbound_order/${id}/pick`, data)
export const shipOutbound = (id, data) => post(`/wms_outbound_order/${id}/ship`, data)

// ===== 盘点闭环 =====
export const createStocktake = (data) => post('/wms_stocktake_order/create', data)
export const freezeStocktake = (id, data) => post(`/wms_stocktake_order/${id}/freeze`, data)
export const confirmStocktake = (id, data) => post(`/wms_stocktake_order/${id}/confirm`, data)
export const countStocktakeItem = (id, data) => post(`/wms_stocktake_item/${id}/count`, data)

/**
 * V1.1 F-06：明细改走「按单据 ID 一次取全量」的新端点。
 *
 * 旧做法是生成器 /page 最多翻 10 页（200/页）再前端筛，超过 2000 行会静默截断——
 * 收货核对现场直接造成少收/错收。新端点在服务端按 order_id / stocktake_id 精确过滤，
 * 没有行数上限，弹窗不再漏行。
 *
 * fetchAll 只保留给库位这类主数据下拉（量小、没有专用端点）。
 */
export async function fetchAll(listFn, predicate) {
  const all = []
  for (let page = 1; page <= 10; page++) {
    const res = await listFn({ page, size: 200 })
    const rows = (res && res.data && res.data.list) || []
    all.push(...rows.filter(r => (predicate ? predicate(r) : true)))
    const total = (res && res.data && res.data.total) || 0
    if (page * 200 >= total) break
  }
  return all
}

export const inboundItems = (orderId) => listOf(`/wms_inbound_order_item/by-order/${orderId}`)
export const outboundItems = (orderId) => listOf(`/wms_outbound_order_item/by-order/${orderId}`)
export const stocktakeItems = (stocktakeId) => listOf(`/wms_stocktake_item/by-stocktake/${stocktakeId}`)
export const locations = () => fetchAll(getWmsLocationList)

/** 状态中文名：与后端 DocStateMachine.LABELS 保持一致 */
const LABELS = {
  CREATED: '已创建', IN_TRANSIT: '在途', RECEIVING: '收货中', RECEIVED: '已收货',
  PUTAWAY_DONE: '已上架', ALLOCATED: '已分配', PARTIAL: '部分满足', PICKING: '拣货中',
  PICKED: '已拣货', SHIPPED: '已发运', FROZEN: '已冻结', CONFIRMED: '已确认',
  COUNTING: '盘点中', CANCELLED: '已取消', DONE: '已完成', PENDING: '待处理'
}
export const statusLabel = (s) => LABELS[s] || s || '—'

/** 状态标签配色：Element UI tag type */
export function statusTagType(s) {
  if (['PUTAWAY_DONE', 'SHIPPED', 'CONFIRMED', 'DONE', 'RECEIVED', 'PICKED', 'ALLOCATED'].includes(s)) return 'success'
  if (['RECEIVING', 'PARTIAL', 'PICKING', 'FROZEN', 'COUNTING'].includes(s)) return 'warning'
  if (['CANCELLED'].includes(s)) return 'danger'
  return 'info'
}

/** 保留 3 位小数但不补无意义的零，与后端 DECIMAL(14,3) 对齐 */
export const qty = (v) => (v === null || v === undefined || v === '' ? 0 : Number(v))
