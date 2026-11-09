package com.wms_warehouse.common.auth;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WmsOutboundOrder 端点-权限映射（代码生成器随 Controller 同源生成）。
 * perms 值与菜单注册/前端 v-hasPermi 同源（wms_warehouse_wms_outbound_order:*）。
 */
@Component
public class WmsOutboundOrderApiPerms implements ApiPermContributor {

    @Override
    public Map<String, String> endpointPerms() {
        Map<String, String> m = new HashMap<>();
        m.put("POST /wms_outbound_order/page", "wms_warehouse_wms_outbound_order:list");
        m.put("GET /wms_outbound_order/tree", "wms_warehouse_wms_outbound_order:list");
        m.put("GET /wms_outbound_order/options", "wms_warehouse_wms_outbound_order:list");
        m.put("GET /wms_outbound_order/bind-values", "wms_warehouse_wms_outbound_order:list");
        m.put("GET /wms_outbound_order/{id}", "wms_warehouse_wms_outbound_order:list");
        m.put("GET /wms_outbound_order/{id}/with-children", "wms_warehouse_wms_outbound_order:list");
        m.put("POST /wms_outbound_order/export", "wms_warehouse_wms_outbound_order:list");
        m.put("GET /wms_outbound_order/import-template", "wms_warehouse_wms_outbound_order:list");
        m.put("GET /wms_outbound_order/import/failed", "wms_warehouse_wms_outbound_order:list");
        m.put("POST /wms_outbound_order", "wms_warehouse_wms_outbound_order:add");
        m.put("PUT /wms_outbound_order", "wms_warehouse_wms_outbound_order:edit");
        m.put("POST /wms_outbound_order/import", "wms_warehouse_wms_outbound_order:add");
        m.put("POST /wms_outbound_order/save-with-children", "wms_warehouse_wms_outbound_order:edit");
        m.put("DELETE /wms_outbound_order/{id}", "wms_warehouse_wms_outbound_order:remove");
        // V1.0 业务闭环端点
        m.put("POST /wms_outbound_order/{id}/allocate", "wms_warehouse_wms_outbound_order:edit");
        m.put("POST /wms_outbound_order/{id}/pick", "wms_warehouse_wms_outbound_order:edit");
        m.put("POST /wms_outbound_order/{id}/ship", "wms_warehouse_wms_outbound_order:edit");
        m.put("POST /wms_outbound_order/{id}/cancel-allocate", "wms_warehouse_wms_outbound_order:edit");
        return m;
    }
}
