package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsStocktakeItem 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_stocktake_item:*）。
 */
@Component
public class WmsStocktakeItemApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_stocktake_item/page", "wms_warehouse_wms_stocktake_item:list");
        m.put("GET /wms_stocktake_item/tree", "wms_warehouse_wms_stocktake_item:list");
        m.put("GET /wms_stocktake_item/options", "wms_warehouse_wms_stocktake_item:list");
        m.put("GET /wms_stocktake_item/bind-values", "wms_warehouse_wms_stocktake_item:list");
        m.put("GET /wms_stocktake_item/{id}", "wms_warehouse_wms_stocktake_item:list");
        m.put("GET /wms_stocktake_item/{id}/with-children", "wms_warehouse_wms_stocktake_item:list");
        m.put("POST /wms_stocktake_item/export", "wms_warehouse_wms_stocktake_item:list");
        m.put("GET /wms_stocktake_item/import-template", "wms_warehouse_wms_stocktake_item:list");
        m.put("GET /wms_stocktake_item/import/failed", "wms_warehouse_wms_stocktake_item:list");
        m.put("POST /wms_stocktake_item", "wms_warehouse_wms_stocktake_item:add");
        m.put("PUT /wms_stocktake_item", "wms_warehouse_wms_stocktake_item:edit");
        m.put("POST /wms_stocktake_item/import", "wms_warehouse_wms_stocktake_item:add");
        m.put("POST /wms_stocktake_item/save-with-children", "wms_warehouse_wms_stocktake_item:edit");
        m.put("DELETE /wms_stocktake_item/{id}", "wms_warehouse_wms_stocktake_item:remove");
        // V1.0 业务闭环端点
        m.put("POST /wms_stocktake_item/{id}/count", "wms_warehouse_wms_stocktake_item:edit");
        return m;
    }
}
