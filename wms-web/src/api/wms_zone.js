import request, { toPageData } from '@/utils/request'

// 库区表接口：wms_zone（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsZoneList(params) {
  return request({
    url: '/api/demo/wms_zone/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsZone(params) {
  return request({
    url: '/api/demo/wms_zone/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsZoneDetail(id) {
  return request({
    url: `/api/demo/wms_zone/${id}`,
    method: 'get'
  })
}
export function createWmsZone(data) {
  return request({
    url: '/api/demo/wms_zone',
    method: 'post',
    data
  })
}
export function updateWmsZone(id, data) {
  return request({
    url: '/api/demo/wms_zone',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsZone(id) {
  return request({
    url: `/api/demo/wms_zone/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====