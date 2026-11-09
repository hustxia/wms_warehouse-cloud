package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsWave 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_wave:*）。
 */
@Component
public class WmsWaveApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_wave/page", "wms_warehouse_wms_wave:list");
        m.put("GET /wms_wave/tree", "wms_warehouse_wms_wave:list");
        m.put("GET /wms_wave/options", "wms_warehouse_wms_wave:list");
        m.put("GET /wms_wave/bind-values", "wms_warehouse_wms_wave:list");
        m.put("GET /wms_wave/{id}", "wms_warehouse_wms_wave:list");
        m.put("GET /wms_wave/{id}/with-children", "wms_warehouse_wms_wave:list");
        m.put("POST /wms_wave/export", "wms_warehouse_wms_wave:list");
        m.put("GET /wms_wave/import-template", "wms_warehouse_wms_wave:list");
        m.put("GET /wms_wave/import/failed", "wms_warehouse_wms_wave:list");
        m.put("POST /wms_wave", "wms_warehouse_wms_wave:add");
        m.put("PUT /wms_wave", "wms_warehouse_wms_wave:edit");
        m.put("POST /wms_wave/import", "wms_warehouse_wms_wave:add");
        m.put("POST /wms_wave/save-with-children", "wms_warehouse_wms_wave:edit");
        m.put("DELETE /wms_wave/{id}", "wms_warehouse_wms_wave:remove");
        return m;
    }
}
