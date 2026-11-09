package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsOwner 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_owner:*）。
 */
@Component
public class WmsOwnerApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_owner/page", "wms_warehouse_wms_owner:list");
        m.put("GET /wms_owner/tree", "wms_warehouse_wms_owner:list");
        m.put("GET /wms_owner/options", "wms_warehouse_wms_owner:list");
        m.put("GET /wms_owner/bind-values", "wms_warehouse_wms_owner:list");
        m.put("GET /wms_owner/{id}", "wms_warehouse_wms_owner:list");
        m.put("GET /wms_owner/{id}/with-children", "wms_warehouse_wms_owner:list");
        m.put("POST /wms_owner/export", "wms_warehouse_wms_owner:list");
        m.put("GET /wms_owner/import-template", "wms_warehouse_wms_owner:list");
        m.put("GET /wms_owner/import/failed", "wms_warehouse_wms_owner:list");
        m.put("POST /wms_owner", "wms_warehouse_wms_owner:add");
        m.put("PUT /wms_owner", "wms_warehouse_wms_owner:edit");
        m.put("POST /wms_owner/import", "wms_warehouse_wms_owner:add");
        m.put("POST /wms_owner/save-with-children", "wms_warehouse_wms_owner:edit");
        m.put("DELETE /wms_owner/{id}", "wms_warehouse_wms_owner:remove");
        return m;
    }
}
