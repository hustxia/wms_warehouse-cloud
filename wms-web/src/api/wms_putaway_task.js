import request, { toPageData } from '@/utils/request'

// 上架任务表接口：wms_putaway_task（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsPutawayTaskList(params) {
  return request({
    url: '/api/demo/wms_putaway_task/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsPutawayTask(params) {
  return request({
    url: '/api/demo/wms_putaway_task/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsPutawayTaskDetail(id) {
  return request({
    url: `/api/demo/wms_putaway_task/${id}`,
    method: 'get'
  })
}
export function createWmsPutawayTask(data) {
  return request({
    url: '/api/demo/wms_putaway_task',
    method: 'post',
    data
  })
}
export function updateWmsPutawayTask(id, data) {
  return request({
    url: '/api/demo/wms_putaway_task',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsPutawayTask(id) {
  return request({
    url: `/api/demo/wms_putaway_task/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====