import request, { toPageData } from '@/utils/request'

// 移库单表接口：wms_transfer_order（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsTransferOrderList(params) {
  return request({
    url: '/api/demo/wms_transfer_order/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsTransferOrder(params) {
  return request({
    url: '/api/demo/wms_transfer_order/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsTransferOrderDetail(id) {
  return request({
    url: `/api/demo/wms_transfer_order/${id}`,
    method: 'get'
  })
}
export function createWmsTransferOrder(data) {
  return request({
    url: '/api/demo/wms_transfer_order',
    method: 'post',
    data
  })
}
export function updateWmsTransferOrder(id, data) {
  return request({
    url: '/api/demo/wms_transfer_order',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsTransferOrder(id) {
  return request({
    url: `/api/demo/wms_transfer_order/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====