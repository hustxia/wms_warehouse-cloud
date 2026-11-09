package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsWarehouse 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_warehouse:*）。
 */
@Component
public class WmsWarehouseApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_warehouse/page", "wms_warehouse_wms_warehouse:list");
        m.put("GET /wms_warehouse/tree", "wms_warehouse_wms_warehouse:list");
        m.put("GET /wms_warehouse/options", "wms_warehouse_wms_warehouse:list");
        m.put("GET /wms_warehouse/bind-values", "wms_warehouse_wms_warehouse:list");
        m.put("GET /wms_warehouse/{id}", "wms_warehouse_wms_warehouse:list");
        m.put("GET /wms_warehouse/{id}/with-children", "wms_warehouse_wms_warehouse:list");
        m.put("POST /wms_warehouse/export", "wms_warehouse_wms_warehouse:list");
        m.put("GET /wms_warehouse/import-template", "wms_warehouse_wms_warehouse:list");
        m.put("GET /wms_warehouse/import/failed", "wms_warehouse_wms_warehouse:list");
        m.put("POST /wms_warehouse", "wms_warehouse_wms_warehouse:add");
        m.put("PUT /wms_warehouse", "wms_warehouse_wms_warehouse:edit");
        m.put("POST /wms_warehouse/import", "wms_warehouse_wms_warehouse:add");
        m.put("POST /wms_warehouse/save-with-children", "wms_warehouse_wms_warehouse:edit");
        m.put("DELETE /wms_warehouse/{id}", "wms_warehouse_wms_warehouse:remove");
        return m;
    }
}
