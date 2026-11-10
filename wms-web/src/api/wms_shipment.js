import request, { toPageData } from '@/utils/request'

// 发货单表接口：wms_shipment（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsShipmentList(params) {
  return request({
    url: '/api/demo/wms_shipment/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsShipment(params) {
  return request({
    url: '/api/demo/wms_shipment/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsShipmentDetail(id) {
  return request({
    url: `/api/demo/wms_shipment/${id}`,
    method: 'get'
  })
}
export function createWmsShipment(data) {
  return request({
    url: '/api/demo/wms_shipment',
    method: 'post',
    data
  })
}
export function updateWmsShipment(id, data) {
  return request({
    url: '/api/demo/wms_shipment',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsShipment(id) {
  return request({
    url: `/api/demo/wms_shipment/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====