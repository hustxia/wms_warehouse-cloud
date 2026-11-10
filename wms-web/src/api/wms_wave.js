import request, { toPageData } from '@/utils/request'

// 波次表接口：wms_wave（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsWaveList(params) {
  return request({
    url: '/api/demo/wms_wave/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsWave(params) {
  return request({
    url: '/api/demo/wms_wave/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsWaveDetail(id) {
  return request({
    url: `/api/demo/wms_wave/${id}`,
    method: 'get'
  })
}
export function createWmsWave(data) {
  return request({
    url: '/api/demo/wms_wave',
    method: 'post',
    data
  })
}
export function updateWmsWave(id, data) {
  return request({
    url: '/api/demo/wms_wave',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsWave(id) {
  return request({
    url: `/api/demo/wms_wave/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====