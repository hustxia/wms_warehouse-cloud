import request, { toPageData } from '@/utils/request'

// 入库单明细表接口：wms_inbound_order_item（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsInboundOrderItemList(params) {
  return request({
    url: '/api/demo/wms_inbound_order_item/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsInboundOrderItem(params) {
  return request({
    url: '/api/demo/wms_inbound_order_item/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsInboundOrderItemDetail(id) {
  return request({
    url: `/api/demo/wms_inbound_order_item/${id}`,
    method: 'get'
  })
}
export function createWmsInboundOrderItem(data) {
  return request({
    url: '/api/demo/wms_inbound_order_item',
    method: 'post',
    data
  })
}
export function updateWmsInboundOrderItem(id, data) {
  return request({
    url: '/api/demo/wms_inbound_order_item',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsInboundOrderItem(id) {
  return request({
    url: `/api/demo/wms_inbound_order_item/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====