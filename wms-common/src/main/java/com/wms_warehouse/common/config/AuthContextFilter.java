package com.wms_warehouse.common.config;

import com.wms_warehouse.common.auth.PermissionChecker;
import com.wms_warehouse.common.auth.TokenStore;
import com.wms_warehouse.common.context.UserContext;
import com.wms_warehouse.common.result.Result;
import com.wms_warehouse.common.result.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 登录上下文填充 + 两级鉴权拦截。
 *
 * <p>V6.4 权限闭环：第一级登录校验（token 有效性，401）；
 * 第二级接口权限点校验（{@link PermissionChecker}，无权 403）——
 * 结束「前端 v-hasPermi 藏按钮、后端裸奔」的历史。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthContextFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private TokenStore tokenStore;

    @Autowired
    private PermissionChecker permissionChecker;

    /** 无需 token 即可访问的路径前缀 */
    private static final List<String> WHITE_LIST = List.of(
            "/auth/login",
            "/auth/captcha",
            "/health",
            "/actuator",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-resources",
            "/error",
            "/img",
            "/system/dict",
            "/shop",
            // 服务间内部端点（V1.0 业务闭环）：网关不路由 /internal/**，只在内网可达；
            // 端点自身校验 X-Internal-Token，不依赖登录态——后台任务/补偿调用没有用户 token
            "/internal"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // CORS 预检不带 Authorization，必须放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        boolean hasToken = StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX);

        if (hasToken) {
            String token = authorization.substring(BEARER_PREFIX.length());
            // 第一级：登录校验（过期/登出即 401）
            String username = tokenStore.usernameOf(token);
            if (username == null) {
                writeUnauthorized(response, "登录状态已失效，请重新登录");
                return;
            }
            // 第二级：接口权限点校验（V6.4 fail-closed：映射缺失读放行写拒绝）
            if (!permissionChecker.check(request, username)) {
                writeForbidden(response, "无该操作权限");
                return;
            }
            UserContext.setUsername(username);
            try {
                filterChain.doFilter(request, response);
            } finally {
                UserContext.clear();
            }
            return;
        }

        if (isWhiteListed(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        writeUnauthorized(response, "未登录或缺少访问令牌");
    }

    private boolean isWhiteListed(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (StringUtils.hasText(contextPath) && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        for (String prefix : WHITE_LIST) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> body = Result.fail(ResultCode.UNAUTHORIZED.getCode(), msg);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private void writeForbidden(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Result<Void> body = Result.fail(ResultCode.FORBIDDEN.getCode(), msg);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
