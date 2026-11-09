package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsInventory 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_inventory:*）。
 */
@Component
public class WmsInventoryApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_inventory/page", "wms_warehouse_wms_inventory:list");
        m.put("GET /wms_inventory/tree", "wms_warehouse_wms_inventory:list");
        m.put("GET /wms_inventory/options", "wms_warehouse_wms_inventory:list");
        m.put("GET /wms_inventory/bind-values", "wms_warehouse_wms_inventory:list");
        m.put("GET /wms_inventory/{id}", "wms_warehouse_wms_inventory:list");
        m.put("GET /wms_inventory/{id}/with-children", "wms_warehouse_wms_inventory:list");
        m.put("POST /wms_inventory/export", "wms_warehouse_wms_inventory:list");
        m.put("GET /wms_inventory/import-template", "wms_warehouse_wms_inventory:list");
        m.put("GET /wms_inventory/import/failed", "wms_warehouse_wms_inventory:list");
        m.put("POST /wms_inventory", "wms_warehouse_wms_inventory:add");
        m.put("PUT /wms_inventory", "wms_warehouse_wms_inventory:edit");
        m.put("POST /wms_inventory/import", "wms_warehouse_wms_inventory:add");
        m.put("POST /wms_inventory/save-with-children", "wms_warehouse_wms_inventory:edit");
        m.put("DELETE /wms_inventory/{id}", "wms_warehouse_wms_inventory:remove");
        return m;
    }
}
