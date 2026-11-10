import request, { toPageData } from '@/utils/request'

// 收货记录表接口：wms_receipt_record（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsReceiptRecordList(params) {
  return request({
    url: '/api/demo/wms_receipt_record/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsReceiptRecord(params) {
  return request({
    url: '/api/demo/wms_receipt_record/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsReceiptRecordDetail(id) {
  return request({
    url: `/api/demo/wms_receipt_record/${id}`,
    method: 'get'
  })
}
export function createWmsReceiptRecord(data) {
  return request({
    url: '/api/demo/wms_receipt_record',
    method: 'post',
    data
  })
}
export function updateWmsReceiptRecord(id, data) {
  return request({
    url: '/api/demo/wms_receipt_record',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsReceiptRecord(id) {
  return request({
    url: `/api/demo/wms_receipt_record/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====