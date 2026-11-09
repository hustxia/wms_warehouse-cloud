package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsInboundOrderItem 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_inbound_order_item:*）。
 */
@Component
public class WmsInboundOrderItemApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_inbound_order_item/page", "wms_warehouse_wms_inbound_order_item:list");
        m.put("GET /wms_inbound_order_item/tree", "wms_warehouse_wms_inbound_order_item:list");
        m.put("GET /wms_inbound_order_item/options", "wms_warehouse_wms_inbound_order_item:list");
        m.put("GET /wms_inbound_order_item/bind-values", "wms_warehouse_wms_inbound_order_item:list");
        m.put("GET /wms_inbound_order_item/{id}", "wms_warehouse_wms_inbound_order_item:list");
        m.put("GET /wms_inbound_order_item/{id}/with-children", "wms_warehouse_wms_inbound_order_item:list");
        m.put("POST /wms_inbound_order_item/export", "wms_warehouse_wms_inbound_order_item:list");
        m.put("GET /wms_inbound_order_item/import-template", "wms_warehouse_wms_inbound_order_item:list");
        m.put("GET /wms_inbound_order_item/import/failed", "wms_warehouse_wms_inbound_order_item:list");
        m.put("POST /wms_inbound_order_item", "wms_warehouse_wms_inbound_order_item:add");
        m.put("PUT /wms_inbound_order_item", "wms_warehouse_wms_inbound_order_item:edit");
        m.put("POST /wms_inbound_order_item/import", "wms_warehouse_wms_inbound_order_item:add");
        m.put("POST /wms_inbound_order_item/save-with-children", "wms_warehouse_wms_inbound_order_item:edit");
        m.put("DELETE /wms_inbound_order_item/{id}", "wms_warehouse_wms_inbound_order_item:remove");
        return m;
    }
}
