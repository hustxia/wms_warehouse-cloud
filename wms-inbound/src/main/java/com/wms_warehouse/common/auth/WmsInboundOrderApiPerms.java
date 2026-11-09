package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsInboundOrder 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_inbound_order:*）。
 */
@Component
public class WmsInboundOrderApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_inbound_order/page", "wms_warehouse_wms_inbound_order:list");
        m.put("GET /wms_inbound_order/tree", "wms_warehouse_wms_inbound_order:list");
        m.put("GET /wms_inbound_order/options", "wms_warehouse_wms_inbound_order:list");
        m.put("GET /wms_inbound_order/bind-values", "wms_warehouse_wms_inbound_order:list");
        m.put("GET /wms_inbound_order/{id}", "wms_warehouse_wms_inbound_order:list");
        m.put("GET /wms_inbound_order/{id}/with-children", "wms_warehouse_wms_inbound_order:list");
        m.put("POST /wms_inbound_order/export", "wms_warehouse_wms_inbound_order:list");
        m.put("GET /wms_inbound_order/import-template", "wms_warehouse_wms_inbound_order:list");
        m.put("GET /wms_inbound_order/import/failed", "wms_warehouse_wms_inbound_order:list");
        m.put("POST /wms_inbound_order", "wms_warehouse_wms_inbound_order:add");
        m.put("PUT /wms_inbound_order", "wms_warehouse_wms_inbound_order:edit");
        m.put("POST /wms_inbound_order/import", "wms_warehouse_wms_inbound_order:add");
        m.put("POST /wms_inbound_order/save-with-children", "wms_warehouse_wms_inbound_order:edit");
        m.put("DELETE /wms_inbound_order/{id}", "wms_warehouse_wms_inbound_order:remove");
        // V1.0 业务闭环端点（手写在 Controller 手写区，权限点复用 edit）
        m.put("POST /wms_inbound_order/{id}/receive", "wms_warehouse_wms_inbound_order:edit");
        m.put("POST /wms_inbound_order/{id}/putaway", "wms_warehouse_wms_inbound_order:edit");
        return m;
    }
}
