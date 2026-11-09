package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsShipment 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_shipment:*）。
 */
@Component
public class WmsShipmentApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_shipment/page", "wms_warehouse_wms_shipment:list");
        m.put("GET /wms_shipment/tree", "wms_warehouse_wms_shipment:list");
        m.put("GET /wms_shipment/options", "wms_warehouse_wms_shipment:list");
        m.put("GET /wms_shipment/bind-values", "wms_warehouse_wms_shipment:list");
        m.put("GET /wms_shipment/{id}", "wms_warehouse_wms_shipment:list");
        m.put("GET /wms_shipment/{id}/with-children", "wms_warehouse_wms_shipment:list");
        m.put("POST /wms_shipment/export", "wms_warehouse_wms_shipment:list");
        m.put("GET /wms_shipment/import-template", "wms_warehouse_wms_shipment:list");
        m.put("GET /wms_shipment/import/failed", "wms_warehouse_wms_shipment:list");
        m.put("POST /wms_shipment", "wms_warehouse_wms_shipment:add");
        m.put("PUT /wms_shipment", "wms_warehouse_wms_shipment:edit");
        m.put("POST /wms_shipment/import", "wms_warehouse_wms_shipment:add");
        m.put("POST /wms_shipment/save-with-children", "wms_warehouse_wms_shipment:edit");
        m.put("DELETE /wms_shipment/{id}", "wms_warehouse_wms_shipment:remove");
        return m;
    }
}
