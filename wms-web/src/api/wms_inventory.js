import request, { toPageData } from '@/utils/request'

// 库存表接口：wms_inventory（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsInventoryList(params) {
  return request({
    url: '/api/demo/wms_inventory/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsInventory(params) {
  return request({
    url: '/api/demo/wms_inventory/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsInventoryDetail(id) {
  return request({
    url: `/api/demo/wms_inventory/${id}`,
    method: 'get'
  })
}
export function createWmsInventory(data) {
  return request({
    url: '/api/demo/wms_inventory',
    method: 'post',
    data
  })
}
export function updateWmsInventory(id, data) {
  return request({
    url: '/api/demo/wms_inventory',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsInventory(id) {
  return request({
    url: `/api/demo/wms_inventory/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====