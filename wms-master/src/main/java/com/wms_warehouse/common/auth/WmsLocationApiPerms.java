package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsLocation 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_location:*）。
 */
@Component
public class WmsLocationApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_location/page", "wms_warehouse_wms_location:list");
        m.put("GET /wms_location/tree", "wms_warehouse_wms_location:list");
        m.put("GET /wms_location/options", "wms_warehouse_wms_location:list");
        m.put("GET /wms_location/bind-values", "wms_warehouse_wms_location:list");
        m.put("GET /wms_location/{id}", "wms_warehouse_wms_location:list");
        m.put("GET /wms_location/{id}/with-children", "wms_warehouse_wms_location:list");
        m.put("POST /wms_location/export", "wms_warehouse_wms_location:list");
        m.put("GET /wms_location/import-template", "wms_warehouse_wms_location:list");
        m.put("GET /wms_location/import/failed", "wms_warehouse_wms_location:list");
        m.put("POST /wms_location", "wms_warehouse_wms_location:add");
        m.put("PUT /wms_location", "wms_warehouse_wms_location:edit");
        m.put("POST /wms_location/import", "wms_warehouse_wms_location:add");
        m.put("POST /wms_location/save-with-children", "wms_warehouse_wms_location:edit");
        m.put("DELETE /wms_location/{id}", "wms_warehouse_wms_location:remove");
        return m;
    }
}
