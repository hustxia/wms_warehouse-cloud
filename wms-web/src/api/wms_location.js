import request, { toPageData } from '@/utils/request'

// 库位表接口：wms_location（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsLocationList(params) {
  return request({
    url: '/api/demo/wms_location/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsLocation(params) {
  return request({
    url: '/api/demo/wms_location/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsLocationDetail(id) {
  return request({
    url: `/api/demo/wms_location/${id}`,
    method: 'get'
  })
}
export function createWmsLocation(data) {
  return request({
    url: '/api/demo/wms_location',
    method: 'post',
    data
  })
}
export function updateWmsLocation(id, data) {
  return request({
    url: '/api/demo/wms_location',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsLocation(id) {
  return request({
    url: `/api/demo/wms_location/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====