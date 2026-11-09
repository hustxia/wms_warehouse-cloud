package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsPickTask 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_pick_task:*）。
 */
@Component
public class WmsPickTaskApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_pick_task/page", "wms_warehouse_wms_pick_task:list");
        m.put("GET /wms_pick_task/tree", "wms_warehouse_wms_pick_task:list");
        m.put("GET /wms_pick_task/options", "wms_warehouse_wms_pick_task:list");
        m.put("GET /wms_pick_task/bind-values", "wms_warehouse_wms_pick_task:list");
        m.put("GET /wms_pick_task/{id}", "wms_warehouse_wms_pick_task:list");
        m.put("GET /wms_pick_task/{id}/with-children", "wms_warehouse_wms_pick_task:list");
        m.put("POST /wms_pick_task/export", "wms_warehouse_wms_pick_task:list");
        m.put("GET /wms_pick_task/import-template", "wms_warehouse_wms_pick_task:list");
        m.put("GET /wms_pick_task/import/failed", "wms_warehouse_wms_pick_task:list");
        m.put("POST /wms_pick_task", "wms_warehouse_wms_pick_task:add");
        m.put("PUT /wms_pick_task", "wms_warehouse_wms_pick_task:edit");
        m.put("POST /wms_pick_task/import", "wms_warehouse_wms_pick_task:add");
        m.put("POST /wms_pick_task/save-with-children", "wms_warehouse_wms_pick_task:edit");
        m.put("DELETE /wms_pick_task/{id}", "wms_warehouse_wms_pick_task:remove");
        return m;
    }
}
