package com.wms_warehouse.service;

import com.wms_warehouse.dto.biz.AllocateRequest;
import com.wms_warehouse.dto.biz.PickRequest;
import com.wms_warehouse.dto.biz.ShipRequest;

import java.util.Map;

/**
 * 出库业务闭环（P0-3）：分配 → 拣货 → 发运。
 *
 * <p>三段各自是一个事务，但共享同一套库存入口。分配只占用（不动在库量），
 * 拣货才真正扣在库量——这样「分了的货还在仓里，拣了才算走」与仓库现场一致。
 */
public interface WmsOutboundBizService {

    /** 分配：按 FEFO 逐行占用可用库存，不足部分留到下次分配 */
    Map<String, Object> allocate(Long orderId, AllocateRequest request);

    /** 拣货确认：扣减在库量与已分配量，写拣货任务与流水 */
    Map<String, Object> pick(Long orderId, PickRequest request);

    /** 发运：写发货单，单据置已发运 */
    Map<String, Object> ship(Long orderId, ShipRequest request);

    /** 取消分配：释放全部占用，单据回到已创建 */
    Map<String, Object> cancelAllocate(Long orderId, String requestId);
}
