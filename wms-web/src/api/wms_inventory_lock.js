import request, { toPageData } from '@/utils/request'

// 库存锁定表接口：wms_inventory_lock（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsInventoryLockList(params) {
  return request({
    url: '/api/demo/wms_inventory_lock/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsInventoryLock(params) {
  return request({
    url: '/api/demo/wms_inventory_lock/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsInventoryLockDetail(id) {
  return request({
    url: `/api/demo/wms_inventory_lock/${id}`,
    method: 'get'
  })
}
export function createWmsInventoryLock(data) {
  return request({
    url: '/api/demo/wms_inventory_lock',
    method: 'post',
    data
  })
}
export function updateWmsInventoryLock(id, data) {
  return request({
    url: '/api/demo/wms_inventory_lock',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsInventoryLock(id) {
  return request({
    url: `/api/demo/wms_inventory_lock/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====