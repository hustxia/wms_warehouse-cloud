/**
 * WMS V1.1 F-02 / F-03：业务字段只读（前端侧）。
 *
 * 后端 ProtectedFieldGuard 对通用 CRUD 的受保护字段是「携带即拒绝」，
 * 所以只把控件设为 disabled 是不够的：disabled 控件的值仍然留在 form 对象里，
 * axios 会一并提交，PUT 立刻被 40001 打回。提交前必须显式剔除。
 *
 * 同时这些字段在生成器产出的 rules 里大多带 required，若不删，
 * 新增弹窗会在 validate() 阶段就失败（见需求文档「边界与异常」）。
 */
export const READONLY_TIP = '由业务端点写入'

/** 与后端 ProtectedFieldGuard 的保护集保持一致（前端多剔除几个纯审计字段） */
export const READONLY_FIELDS = {
  wms_inbound_order: ['status', 'operatorId', 'createdAt', 'updatedAt'],
  wms_outbound_order: ['status', 'carrier', 'trackingNo', 'createdAt', 'updatedAt'],
  wms_stocktake_order: ['status', 'createdBy', 'createdAt', 'confirmedAt'],
  wms_inventory: ['qtyOnHand', 'qtyAllocated', 'qtyLocked', 'qtyAvailable', 'version', 'updatedAt']
}

/**
 * 生成提交用的 payload：去掉受保护字段，保留 id 与所有可编辑业务键。
 * 不改原 form，避免弹窗里的展示值被清空。
 */
export function stripReadonly(form, resource) {
  const payload = { ...form }
  ;(READONLY_FIELDS[resource] || []).forEach((field) => {
    delete payload[field]
  })
  return payload
}
