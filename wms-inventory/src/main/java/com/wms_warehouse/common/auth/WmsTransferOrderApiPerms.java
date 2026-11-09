package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsTransferOrder 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_transfer_order:*）。
 */
@Component
public class WmsTransferOrderApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_transfer_order/page", "wms_warehouse_wms_transfer_order:list");
        m.put("GET /wms_transfer_order/tree", "wms_warehouse_wms_transfer_order:list");
        m.put("GET /wms_transfer_order/options", "wms_warehouse_wms_transfer_order:list");
        m.put("GET /wms_transfer_order/bind-values", "wms_warehouse_wms_transfer_order:list");
        m.put("GET /wms_transfer_order/{id}", "wms_warehouse_wms_transfer_order:list");
        m.put("GET /wms_transfer_order/{id}/with-children", "wms_warehouse_wms_transfer_order:list");
        m.put("POST /wms_transfer_order/export", "wms_warehouse_wms_transfer_order:list");
        m.put("GET /wms_transfer_order/import-template", "wms_warehouse_wms_transfer_order:list");
        m.put("GET /wms_transfer_order/import/failed", "wms_warehouse_wms_transfer_order:list");
        m.put("POST /wms_transfer_order", "wms_warehouse_wms_transfer_order:add");
        m.put("PUT /wms_transfer_order", "wms_warehouse_wms_transfer_order:edit");
        m.put("POST /wms_transfer_order/import", "wms_warehouse_wms_transfer_order:add");
        m.put("POST /wms_transfer_order/save-with-children", "wms_warehouse_wms_transfer_order:edit");
        m.put("DELETE /wms_transfer_order/{id}", "wms_warehouse_wms_transfer_order:remove");
        return m;
    }
}
