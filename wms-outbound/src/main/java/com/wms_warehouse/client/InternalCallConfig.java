package com.wms_warehouse.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign 内部调用配置：加内网令牌头。
 *
 * <p>刻意不加 {@code @Configuration}——带上它会变成全局 Feign 配置，
 * 把内部令牌塞进所有外部调用。
 */
public class InternalCallConfig {

    @Value("${app.internal.token}")
    private String internalToken;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return template -> template.header("X-Internal-Token", internalToken);
    }
}
