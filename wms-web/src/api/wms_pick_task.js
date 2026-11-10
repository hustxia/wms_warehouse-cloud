import request, { toPageData } from '@/utils/request'

// 拣货任务表接口：wms_pick_task（经网关 /api/demo）
// <gen:begin key="api-fns">
function toPageQuery(params) {
  const { page, size, ...query } = params || {}
  return { pageNum: page || 1, pageSize: size || 10, ...query }
}
export function getWmsPickTaskList(params) {
  return request({
    url: '/api/demo/wms_pick_task/page',
    method: 'post',
    data: toPageQuery(params)
  }).then(toPageData)
}
export function exportWmsPickTask(params) {
  return request({
    url: '/api/demo/wms_pick_task/export',
    method: 'post',
    data: params || {},
    responseType: 'blob'
  })
}
export function getWmsPickTaskDetail(id) {
  return request({
    url: `/api/demo/wms_pick_task/${id}`,
    method: 'get'
  })
}
export function createWmsPickTask(data) {
  return request({
    url: '/api/demo/wms_pick_task',
    method: 'post',
    data
  })
}
export function updateWmsPickTask(id, data) {
  return request({
    url: '/api/demo/wms_pick_task',
    method: 'put',
    data: { ...data, id }
  })
}
export function deleteWmsPickTask(id) {
  return request({
    url: `/api/demo/wms_pick_task/${id}`,
    method: 'delete'
  })
}
// <gen:end key="api-fns">
// ===== 以下为手写区：自定义请求写在这里，重新生成不会被覆盖 =====