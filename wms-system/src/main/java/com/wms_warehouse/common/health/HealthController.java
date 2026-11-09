package com.wms_warehouse.common.health;

import com.wms_warehouse.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查端点（免登录，AuthContextFilter 白名单已含 /health）。
 *
 * 网关把 /health 路由到 wms-system；前端首页「后端连通检测」按全局返回体
 * res.code === 200 判成功，所以这里必须包 Result.ok —— 返回裸 Map/String
 * 会触发坑库《spring-boot-3》「字典只读接口返回裸数组，每个页面弹请求失败」同款问题。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("service", "wms-system");
        data.put("time", LocalDateTime.now().toString());
        return Result.ok(data);
    }
}
