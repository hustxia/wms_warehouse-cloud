import request, { toPageData } from '@/utils/request'

// 出库单主表接口：wms_outbound_order（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsOutboundOrderList(params) {
  return request({
    url: '/api/demo/wms_outbound_order/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsOutboundOrder(params) {
  return request({
    url: '/api/demo/wms_outbound_order/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsOutboundOrderDetail(id) {
  return request({
    url: `/api/demo/wms_outbound_order/${id}`,
    method: 'get'
  })
}
export function createWmsOutboundOrder(data) {
  return request({
    url: '/api/demo/wms_outbound_order',
    method: 'post',
    data
  })
}
export function updateWmsOutboundOrder(id, data) {
  return request({
    url: '/api/demo/wms_outbound_order',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsOutboundOrder(id) {
  return request({
    url: `/api/demo/wms_outbound_order/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====