package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsReceiptRecord 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_receipt_record:*）。
 */
@Component
public class WmsReceiptRecordApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_receipt_record/page", "wms_warehouse_wms_receipt_record:list");
        m.put("GET /wms_receipt_record/tree", "wms_warehouse_wms_receipt_record:list");
        m.put("GET /wms_receipt_record/options", "wms_warehouse_wms_receipt_record:list");
        m.put("GET /wms_receipt_record/bind-values", "wms_warehouse_wms_receipt_record:list");
        m.put("GET /wms_receipt_record/{id}", "wms_warehouse_wms_receipt_record:list");
        m.put("GET /wms_receipt_record/{id}/with-children", "wms_warehouse_wms_receipt_record:list");
        m.put("POST /wms_receipt_record/export", "wms_warehouse_wms_receipt_record:list");
        m.put("GET /wms_receipt_record/import-template", "wms_warehouse_wms_receipt_record:list");
        m.put("GET /wms_receipt_record/import/failed", "wms_warehouse_wms_receipt_record:list");
        m.put("POST /wms_receipt_record", "wms_warehouse_wms_receipt_record:add");
        m.put("PUT /wms_receipt_record", "wms_warehouse_wms_receipt_record:edit");
        m.put("POST /wms_receipt_record/import", "wms_warehouse_wms_receipt_record:add");
        m.put("POST /wms_receipt_record/save-with-children", "wms_warehouse_wms_receipt_record:edit");
        m.put("DELETE /wms_receipt_record/{id}", "wms_warehouse_wms_receipt_record:remove");
        return m;
    }
}
