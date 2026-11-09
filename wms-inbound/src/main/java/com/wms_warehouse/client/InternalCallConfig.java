package com.wms_warehouse.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Feign 内部调用配置：加内网令牌头。
 *
 * <p>刻意**不加** {@code @Configuration}：Feign 的 {@code configuration} 属性会为这个客户端
 * 建独立子上下文，一旦带上该注解就会变成全局配置，把所有 Feign 调用都塞上内部令牌。
 */
public class InternalCallConfig {

    @Value("${app.internal.token}")
    private String internalToken;

    @Bean
    public RequestInterceptor internalTokenInterceptor() {
        return template -> template.header("X-Internal-Token", internalToken);
    }
}
