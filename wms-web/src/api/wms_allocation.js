import request, { toPageData } from '@/utils/request'

// 库存分配明细表接口：wms_allocation（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsAllocationList(params) {
  return request({
    url: '/api/demo/wms_allocation/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsAllocation(params) {
  return request({
    url: '/api/demo/wms_allocation/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsAllocationDetail(id) {
  return request({
    url: `/api/demo/wms_allocation/${id}`,
    method: 'get'
  })
}
export function createWmsAllocation(data) {
  return request({
    url: '/api/demo/wms_allocation',
    method: 'post',
    data
  })
}
export function updateWmsAllocation(id, data) {
  return request({
    url: '/api/demo/wms_allocation',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsAllocation(id) {
  return request({
    url: `/api/demo/wms_allocation/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====