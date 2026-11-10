import request, { toPageData } from '@/utils/request'

// 库存事务流水表接口：wms_inventory_txn（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsInventoryTxnList(params) {
  return request({
    url: '/api/demo/wms_inventory_txn/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsInventoryTxn(params) {
  return request({
    url: '/api/demo/wms_inventory_txn/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsInventoryTxnDetail(id) {
  return request({
    url: `/api/demo/wms_inventory_txn/${id}`,
    method: 'get'
  })
}
export function createWmsInventoryTxn(data) {
  return request({
    url: '/api/demo/wms_inventory_txn',
    method: 'post',
    data
  })
}
export function updateWmsInventoryTxn(id, data) {
  return request({
    url: '/api/demo/wms_inventory_txn',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsInventoryTxn(id) {
  return request({
    url: `/api/demo/wms_inventory_txn/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====