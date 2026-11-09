package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsZone 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_zone:*）。
 */
@Component
public class WmsZoneApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_zone/page", "wms_warehouse_wms_zone:list");
        m.put("GET /wms_zone/tree", "wms_warehouse_wms_zone:list");
        m.put("GET /wms_zone/options", "wms_warehouse_wms_zone:list");
        m.put("GET /wms_zone/bind-values", "wms_warehouse_wms_zone:list");
        m.put("GET /wms_zone/{id}", "wms_warehouse_wms_zone:list");
        m.put("GET /wms_zone/{id}/with-children", "wms_warehouse_wms_zone:list");
        m.put("POST /wms_zone/export", "wms_warehouse_wms_zone:list");
        m.put("GET /wms_zone/import-template", "wms_warehouse_wms_zone:list");
        m.put("GET /wms_zone/import/failed", "wms_warehouse_wms_zone:list");
        m.put("POST /wms_zone", "wms_warehouse_wms_zone:add");
        m.put("PUT /wms_zone", "wms_warehouse_wms_zone:edit");
        m.put("POST /wms_zone/import", "wms_warehouse_wms_zone:add");
        m.put("POST /wms_zone/save-with-children", "wms_warehouse_wms_zone:edit");
        m.put("DELETE /wms_zone/{id}", "wms_warehouse_wms_zone:remove");
        return m;
    }
}
