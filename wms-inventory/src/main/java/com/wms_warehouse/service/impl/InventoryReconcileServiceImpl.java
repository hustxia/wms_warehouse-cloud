package com.wms_warehouse.service.impl;

import com.wms_warehouse.common.idempotent.WmsIdempotentService;
import com.wms_warehouse.dto.biz.ReconcileAdjustRequest;
import com.wms_warehouse.entity.WmsInventoryTxn;
import com.wms_warehouse.service.InventoryCoreService;
import com.wms_warehouse.service.InventoryReconcileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @see InventoryReconcileService
 */
@Service
public class InventoryReconcileServiceImpl implements InventoryReconcileService {

    private static final String ACTION = "RECONCILE_ADJUST";

    @Autowired
    private WmsIdempotentService idempotentService;

    @Autowired
    private InventoryCoreService inventoryCoreService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> adjust(ReconcileAdjustRequest request) {
        Map<String, Object> replay = idempotentService.beginOrReplay(
                request.getRequestId(), ACTION, String.valueOf(request.getInventoryId()));
        if (replay != null) {
            return replay;
        }
        String refNo = StringUtils.hasText(request.getRefNo())
                ? request.getRefNo()
                : "RECONCILE-" + request.getRequestId();
        WmsInventoryTxn txn = inventoryCoreService.reconcileAdjust(
                request.getInventoryId(), request.getTargetOnHand(), refNo, request.getRemark());

        Map<String, Object> result = new LinkedHashMap<>();
        // 雪花 ID 必须字符串化，否则前端 JSON.parse 丢精度（坑库《前端与流程时序》）
        result.put("inventoryId", String.valueOf(request.getInventoryId()));
        result.put("txnId", String.valueOf(txn.getId()));
        result.put("before", txn.getQtyBefore());
        result.put("after", txn.getQtyAfter());
        result.put("qtyChange", txn.getQtyChange());
        result.put("replayed", false);
        idempotentService.finish(request.getRequestId(), result);
        return result;
    }
}
