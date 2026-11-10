import request, { toPageData } from '@/utils/request'

// 仓库主表接口：wms_warehouse（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsWarehouseList(params) {
  return request({
    url: '/api/demo/wms_warehouse/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsWarehouse(params) {
  return request({
    url: '/api/demo/wms_warehouse/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsWarehouseDetail(id) {
  return request({
    url: `/api/demo/wms_warehouse/${id}`,
    method: 'get'
  })
}
export function createWmsWarehouse(data) {
  return request({
    url: '/api/demo/wms_warehouse',
    method: 'post',
    data
  })
}
export function updateWmsWarehouse(id, data) {
  return request({
    url: '/api/demo/wms_warehouse',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsWarehouse(id) {
  return request({
    url: `/api/demo/wms_warehouse/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====