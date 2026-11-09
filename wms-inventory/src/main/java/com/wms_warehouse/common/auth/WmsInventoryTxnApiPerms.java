package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsInventoryTxn 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_inventory_txn:*）。
 */
@Component
public class WmsInventoryTxnApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_inventory_txn/page", "wms_warehouse_wms_inventory_txn:list");
        m.put("GET /wms_inventory_txn/tree", "wms_warehouse_wms_inventory_txn:list");
        m.put("GET /wms_inventory_txn/options", "wms_warehouse_wms_inventory_txn:list");
        m.put("GET /wms_inventory_txn/bind-values", "wms_warehouse_wms_inventory_txn:list");
        m.put("GET /wms_inventory_txn/{id}", "wms_warehouse_wms_inventory_txn:list");
        m.put("GET /wms_inventory_txn/{id}/with-children", "wms_warehouse_wms_inventory_txn:list");
        m.put("POST /wms_inventory_txn/export", "wms_warehouse_wms_inventory_txn:list");
        m.put("GET /wms_inventory_txn/import-template", "wms_warehouse_wms_inventory_txn:list");
        m.put("GET /wms_inventory_txn/import/failed", "wms_warehouse_wms_inventory_txn:list");
        m.put("POST /wms_inventory_txn", "wms_warehouse_wms_inventory_txn:add");
        m.put("PUT /wms_inventory_txn", "wms_warehouse_wms_inventory_txn:edit");
        m.put("POST /wms_inventory_txn/import", "wms_warehouse_wms_inventory_txn:add");
        m.put("POST /wms_inventory_txn/save-with-children", "wms_warehouse_wms_inventory_txn:edit");
        m.put("DELETE /wms_inventory_txn/{id}", "wms_warehouse_wms_inventory_txn:remove");
        return m;
    }
}
