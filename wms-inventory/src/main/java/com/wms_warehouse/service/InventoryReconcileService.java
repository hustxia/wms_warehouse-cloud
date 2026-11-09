package com.wms_warehouse.service;

import com.wms_warehouse.dto.biz.ReconcileAdjustRequest;

import java.util.Map;

/**
 * 对账冲销编排（V1.1 F-08）。
 *
 * <p>把「幂等占位」和「库存校正 + 写流水」放在<b>同一个本地事务</b>里。
 * 若在 Controller 里先占位再调服务，服务失败时占位行已经单独提交，
 * 用户重试会永远拿到「请求正在处理中」——坑库《前端与流程时序》坑 5 的同型问题。
 */
public interface InventoryReconcileService {

    /**
     * @return 冲销结果；重复 requestId 直接回放上次结果并带 {@code replayed=true}
     */
    Map<String, Object> adjust(ReconcileAdjustRequest request);
}
