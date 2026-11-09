package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsSku 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_sku:*）。
 */
@Component
public class WmsSkuApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_sku/page", "wms_warehouse_wms_sku:list");
        m.put("GET /wms_sku/tree", "wms_warehouse_wms_sku:list");
        m.put("GET /wms_sku/options", "wms_warehouse_wms_sku:list");
        m.put("GET /wms_sku/bind-values", "wms_warehouse_wms_sku:list");
        m.put("GET /wms_sku/{id}", "wms_warehouse_wms_sku:list");
        m.put("GET /wms_sku/{id}/with-children", "wms_warehouse_wms_sku:list");
        m.put("POST /wms_sku/export", "wms_warehouse_wms_sku:list");
        m.put("GET /wms_sku/import-template", "wms_warehouse_wms_sku:list");
        m.put("GET /wms_sku/import/failed", "wms_warehouse_wms_sku:list");
        m.put("POST /wms_sku", "wms_warehouse_wms_sku:add");
        m.put("PUT /wms_sku", "wms_warehouse_wms_sku:edit");
        m.put("POST /wms_sku/import", "wms_warehouse_wms_sku:add");
        m.put("POST /wms_sku/save-with-children", "wms_warehouse_wms_sku:edit");
        m.put("DELETE /wms_sku/{id}", "wms_warehouse_wms_sku:remove");
        return m;
    }
}
