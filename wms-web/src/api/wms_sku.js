import request, { toPageData } from '@/utils/request'

// 商品SKU表接口：wms_sku（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsSkuList(params) {
  return request({
    url: '/api/demo/wms_sku/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsSku(params) {
  return request({
    url: '/api/demo/wms_sku/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsSkuDetail(id) {
  return request({
    url: `/api/demo/wms_sku/${id}`,
    method: 'get'
  })
}
export function createWmsSku(data) {
  return request({
    url: '/api/demo/wms_sku',
    method: 'post',
    data
  })
}
export function updateWmsSku(id, data) {
  return request({
    url: '/api/demo/wms_sku',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsSku(id) {
  return request({
    url: `/api/demo/wms_sku/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====