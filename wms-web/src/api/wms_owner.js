import request, { toPageData } from '@/utils/request'

// 货主表接口：wms_owner（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsOwnerList(params) {
  return request({
    url: '/api/demo/wms_owner/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsOwner(params) {
  return request({
    url: '/api/demo/wms_owner/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsOwnerDetail(id) {
  return request({
    url: `/api/demo/wms_owner/${id}`,
    method: 'get'
  })
}
export function createWmsOwner(data) {
  return request({
    url: '/api/demo/wms_owner',
    method: 'post',
    data
  })
}
export function updateWmsOwner(id, data) {
  return request({
    url: '/api/demo/wms_owner',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsOwner(id) {
  return request({
    url: `/api/demo/wms_owner/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====