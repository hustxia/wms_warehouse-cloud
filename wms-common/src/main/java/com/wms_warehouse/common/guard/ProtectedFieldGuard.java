package com.wms_warehouse.common.guard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

/**
 * 业务字段只读的后端防线（WMS V1.1 F-03）。
 *
 * <p><b>为什么放在 common 而不是逐 Controller 改</b>：20+ 个 Controller 的新增/修改方法
 * 全部是代码生成器产物（{@code BeanUtils.copyProperties} 全量拷贝），改了下次重新生成就被覆盖。
 * 用 {@link RequestBodyAdvice} 在请求体反序列化之前按「路径 + 原始 JSON key」拦截，
 * 一处生效、对所有生成区方法都成立，业务端点的子路径不受影响。
 *
 * <p><b>为什么看原始 JSON 而不是反序列化后的 DTO</b>：DTO 无法区分「客户端没传」和
 * 「客户端传了 null」。只读规则要求「携带即拒绝」，否则 {@code {"qtyOnHand": null}}
 * 配合 MyBatis-Plus 的部分更新策略仍可能成为绕过口子。
 *
 * <p>拦截范围只限 4 类单据/库存资源的<b>根路径</b> {@code POST}/{@code PUT}：
 * 主数据（如 {@code /wms_warehouse} 的启用停用 status）放行，
 * 业务端点（{@code /receive}、{@code /putaway}、{@code /pick}、{@code /{id}/count}）放行。
 */
@ControllerAdvice
public class ProtectedFieldGuard implements RequestBodyAdvice {

    private static final Map<String, Set<String>> PROTECTED_FIELDS = Map.of(
            "wms_inbound_order", Set.of("status"),
            "wms_outbound_order", Set.of("status", "carrier", "trackingNo"),
            "wms_stocktake_order", Set.of("status", "confirmedAt"),
            "wms_inventory", Set.of("qtyOnHand", "qtyAllocated", "qtyLocked", "qtyAvailable", "version")
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return !protectedFieldsOf().isEmpty();
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter,
                                           Type targetType,
                                           Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        Set<String> protectedFields = protectedFieldsOf();
        byte[] body = inputMessage.getBody().readAllBytes();
        // 必须把流还回去：读过的 InputStream 不能再给 Jackson 用
        HttpInputMessage cached = new CachedBodyInputMessage(body, inputMessage.getHeaders());
        if (body.length == 0 || protectedFields.isEmpty()) {
            return cached;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            // 非 JSON 由消息转换器自己报错，守卫不越权
            return cached;
        }
        if (root != null && root.isObject()) {
            for (String field : protectedFields) {
                if (root.has(field)) {
                    throw new BusinessException(BizCode.FIELD_READONLY,
                            "字段 " + field + " 由业务端点写入，禁止通过通用接口直接修改");
                }
            }
        }
        return cached;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType,
                                Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                  Type targetType,
                                  Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    /** 按 URI 最后一段精确匹配资源名；带子路径的业务端点天然不匹配。 */
    private static Set<String> protectedFieldsOf() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return Set.of();
        }
        HttpServletRequest request = attrs.getRequest();
        String method = request.getMethod();
        if (!"POST".equals(method) && !"PUT".equals(method)) {
            return Set.of();
        }
        String uri = request.getRequestURI();
        while (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        String resource = uri.substring(uri.lastIndexOf('/') + 1);
        return PROTECTED_FIELDS.getOrDefault(resource, Set.of());
    }

    /** 可重复读的请求体。 */
    private record CachedBodyInputMessage(byte[] body, HttpHeaders headers) implements HttpInputMessage {
        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }
}
