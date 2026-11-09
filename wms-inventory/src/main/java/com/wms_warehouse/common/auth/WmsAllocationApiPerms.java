package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsAllocation 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_allocation:*）。
 */
@Component
public class WmsAllocationApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_allocation/page", "wms_warehouse_wms_allocation:list");
        m.put("GET /wms_allocation/tree", "wms_warehouse_wms_allocation:list");
        m.put("GET /wms_allocation/options", "wms_warehouse_wms_allocation:list");
        m.put("GET /wms_allocation/bind-values", "wms_warehouse_wms_allocation:list");
        m.put("GET /wms_allocation/{id}", "wms_warehouse_wms_allocation:list");
        m.put("GET /wms_allocation/{id}/with-children", "wms_warehouse_wms_allocation:list");
        m.put("POST /wms_allocation/export", "wms_warehouse_wms_allocation:list");
        m.put("GET /wms_allocation/import-template", "wms_warehouse_wms_allocation:list");
        m.put("GET /wms_allocation/import/failed", "wms_warehouse_wms_allocation:list");
        m.put("POST /wms_allocation", "wms_warehouse_wms_allocation:add");
        m.put("PUT /wms_allocation", "wms_warehouse_wms_allocation:edit");
        m.put("POST /wms_allocation/import", "wms_warehouse_wms_allocation:add");
        m.put("POST /wms_allocation/save-with-children", "wms_warehouse_wms_allocation:edit");
        m.put("DELETE /wms_allocation/{id}", "wms_warehouse_wms_allocation:remove");
        return m;
    }
}
