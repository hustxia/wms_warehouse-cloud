package com.wms_warehouse.common.log;

import com.wms_warehouse.common.context.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 操作日志切面。
 *
 * <p>日志写失败绝不影响主流程；参数落库前按字段名脱敏；超长内容截断。
 */
@Aspect
@Component
public class LogAspect {

    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 单字段上限：够定位问题，又不至于把表塞满
     */
    private static final int MAX_TEXT = 2000;

    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)\"(password|passwd|pwd|token|secret|authorization|credential)\"\\s*:\\s*"
                    + "(\"(?:[^\"\\\\]|\\\\.)*\"|[^,}\\]\\s]+)");

    @Autowired
    private OpeLogMapper opeLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IpRegionResolver ipRegionResolver;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint point, Log operLog) throws Throwable {
        long start = System.currentTimeMillis();
        String errorMsg = null;
        Object result = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable e) {
            errorMsg = e.getMessage();
            throw e;
        } finally {
            try {
                save(point, operLog, result, errorMsg, System.currentTimeMillis() - start);
            } catch (Exception e) {
                // 记录日志失败不能反向影响业务
                log.warn("操作日志写入失败：{}", e.getMessage());
            }
        }
    }

    private void save(ProceedingJoinPoint point, Log operLog, Object result,
                      String errorMsg, long costMs) {
        OpeLog entity = new OpeLog();
        entity.setTitle(operLog.title());
        entity.setStatus(errorMsg == null ? 1 : 0);
        entity.setErrorMsg(truncate(errorMsg));
        entity.setCostMs(costMs);
        entity.setOperTime(LocalDateTime.now());
        entity.setOperUserId(UserContext.getUserId());
        entity.setOperName(UserContext.getUsername());

        MethodSignature signature = (MethodSignature) point.getSignature();
        entity.setMethod(signature.getDeclaringType().getSimpleName() + "#" + signature.getName());

        HttpServletRequest request = currentRequest();
        String ip = null;
        if (request != null) {
            entity.setRequestUri(request.getRequestURI());
            entity.setRequestMethod(request.getMethod());
            ip = clientIp(request);
            entity.setOperIp(ip);
        }
        entity.setOperType(inferType(operLog.type(), request, signature.getName()));
        if (operLog.saveParam()) {
            entity.setOperParam(truncate(toJson(sanitize(point.getArgs()))));
        }
        if (operLog.saveResult() && result != null) {
            entity.setJsonResult(truncate(toJson(result)));
        }
        opeLogMapper.insert(entity);

        // 省市由 IP 反查：审计时只看 IP 判断不了地域，但查询要走网络，
        // 必须异步回填，否则每条日志都让业务接口慢一次网络往返
        if (ip != null) {
            Long logId = entity.getId();
            ipRegionResolver.resolveAsync(ip, region -> {
                OpeLog patch = new OpeLog();
                patch.setId(logId);
                patch.setProvince(region.province());
                patch.setCity(region.city());
                opeLogMapper.updateById(patch);
            });
        }
    }

    /**
     * 文件流、Servlet 对象无法序列化，替换成类型占位
     */
    private List<Object> sanitize(Object[] args) {
        List<Object> list = new ArrayList<>();
        if (args == null) {
            return list;
        }
        for (Object arg : args) {
            if (arg instanceof MultipartFile file) {
                list.add("<file:" + file.getOriginalFilename() + ", " + file.getSize() + "B>");
            } else if (arg instanceof MultipartFile[] files) {
                list.add("<files:" + files.length + ">");
            } else if (arg instanceof jakarta.servlet.ServletRequest
                    || arg instanceof jakarta.servlet.ServletResponse) {
                list.add("<servlet>");
            } else {
                list.add(arg);
            }
        }
        return list;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return maskSensitive(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    /**
     * 按字段名脱敏：只换值不换键，正常业务文本里出现 password 三个字不会被误伤。
     * 匹配必须连值一起：只替换字段名会留下 {"password":"****":"真实口令"}。
     */
    static String maskSensitive(String json) {
        if (json == null) {
            return null;
        }
        return SENSITIVE.matcher(json).replaceAll(m -> "\"" + m.group(1) + "\":\"****\"");
    }

    private String inferType(String declared, HttpServletRequest request, String methodName) {
        if (declared != null && !declared.isBlank()) {
            return declared.toUpperCase(Locale.ROOT);
        }
        String m = methodName.toLowerCase(Locale.ROOT);
        if (m.contains("import")) {
            return "IMPORT";
        }
        if (m.contains("export")) {
            return "EXPORT";
        }
        if (request == null) {
            return "OTHER";
        }
        return switch (request.getMethod().toUpperCase(Locale.ROOT)) {
            case "POST" -> "INSERT";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String clientIp(HttpServletRequest request) {
        for (String header : Arrays.asList("X-Forwarded-For", "X-Real-IP")) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                // 多级代理时第一个才是客户端
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) + "...(truncated)" : text;
    }
}
