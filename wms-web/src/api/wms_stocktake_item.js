import request, { toPageData } from '@/utils/request'

// 盘点明细表接口：wms_stocktake_item（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsStocktakeItemList(params) {
  return request({
    url: '/api/demo/wms_stocktake_item/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsStocktakeItem(params) {
  return request({
    url: '/api/demo/wms_stocktake_item/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsStocktakeItemDetail(id) {
  return request({
    url: `/api/demo/wms_stocktake_item/${id}`,
    method: 'get'
  })
}
export function createWmsStocktakeItem(data) {
  return request({
    url: '/api/demo/wms_stocktake_item',
    method: 'post',
    data
  })
}
export function updateWmsStocktakeItem(id, data) {
  return request({
    url: '/api/demo/wms_stocktake_item',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsStocktakeItem(id) {
  return request({
    url: `/api/demo/wms_stocktake_item/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====