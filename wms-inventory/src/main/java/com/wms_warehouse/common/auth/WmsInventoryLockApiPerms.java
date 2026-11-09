package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsInventoryLock 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_inventory_lock:*）。
 */
@Component
public class WmsInventoryLockApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_inventory_lock/page", "wms_warehouse_wms_inventory_lock:list");
        m.put("GET /wms_inventory_lock/tree", "wms_warehouse_wms_inventory_lock:list");
        m.put("GET /wms_inventory_lock/options", "wms_warehouse_wms_inventory_lock:list");
        m.put("GET /wms_inventory_lock/bind-values", "wms_warehouse_wms_inventory_lock:list");
        m.put("GET /wms_inventory_lock/{id}", "wms_warehouse_wms_inventory_lock:list");
        m.put("GET /wms_inventory_lock/{id}/with-children", "wms_warehouse_wms_inventory_lock:list");
        m.put("POST /wms_inventory_lock/export", "wms_warehouse_wms_inventory_lock:list");
        m.put("GET /wms_inventory_lock/import-template", "wms_warehouse_wms_inventory_lock:list");
        m.put("GET /wms_inventory_lock/import/failed", "wms_warehouse_wms_inventory_lock:list");
        m.put("POST /wms_inventory_lock", "wms_warehouse_wms_inventory_lock:add");
        m.put("PUT /wms_inventory_lock", "wms_warehouse_wms_inventory_lock:edit");
        m.put("POST /wms_inventory_lock/import", "wms_warehouse_wms_inventory_lock:add");
        m.put("POST /wms_inventory_lock/save-with-children", "wms_warehouse_wms_inventory_lock:edit");
        m.put("DELETE /wms_inventory_lock/{id}", "wms_warehouse_wms_inventory_lock:remove");
        return m;
    }
}
