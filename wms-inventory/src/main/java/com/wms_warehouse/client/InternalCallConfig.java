package com.wms_warehouse.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign 内部调用配置：加内网令牌头（不加 {@code @Configuration}，避免变成全局配置）。
 */
public class InternalCallConfig {

    @Value("${app.internal.token}")
    private String internalToken;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return template -> template.header("X-Internal-Token", internalToken);
    }
}
