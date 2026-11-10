import request, { toPageData } from '@/utils/request'

// 盘点单表接口：wms_stocktake_order（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsStocktakeOrderList(params) {
  return request({
    url: '/api/demo/wms_stocktake_order/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsStocktakeOrder(params) {
  return request({
    url: '/api/demo/wms_stocktake_order/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsStocktakeOrderDetail(id) {
  return request({
    url: `/api/demo/wms_stocktake_order/${id}`,
    method: 'get'
  })
}
export function createWmsStocktakeOrder(data) {
  return request({
    url: '/api/demo/wms_stocktake_order',
    method: 'post',
    data
  })
}
export function updateWmsStocktakeOrder(id, data) {
  return request({
    url: '/api/demo/wms_stocktake_order',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsStocktakeOrder(id) {
  return request({
    url: `/api/demo/wms_stocktake_order/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====