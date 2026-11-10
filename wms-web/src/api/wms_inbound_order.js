import request, { toPageData } from '@/utils/request'

// 入库单主表接口：wms_inbound_order（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsInboundOrderList(params) {
  return request({
    url: '/api/demo/wms_inbound_order/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsInboundOrder(params) {
  return request({
    url: '/api/demo/wms_inbound_order/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsInboundOrderDetail(id) {
  return request({
    url: `/api/demo/wms_inbound_order/${id}`,
    method: 'get'
  })
}
export function createWmsInboundOrder(data) {
  return request({
    url: '/api/demo/wms_inbound_order',
    method: 'post',
    data
  })
}
export function updateWmsInboundOrder(id, data) {
  return request({
    url: '/api/demo/wms_inbound_order',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsInboundOrder(id) {
  return request({
    url: `/api/demo/wms_inbound_order/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====