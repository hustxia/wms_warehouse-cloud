package com.wms_warehouse.common.idempotent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 幂等键实现：先占位（唯一键 uk_request 保证全局唯一），执行完回写结果。
 *
 * <p>不要在外层 catch 里再包事务回滚——{@link DuplicateKeyException} 只让当前语句失败，
 * 事务本身仍可用；若让异常穿出 @Transactional 代理边界，事务会被标记 rollback-only，
 * 用户重试时永远拿不到「重复请求返回上次结果」。
 */
@Slf4j
@Service
public class WmsIdempotentServiceImpl implements WmsIdempotentService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    @Autowired
    private WmsRequestLogMapper requestLogMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> beginOrReplay(String requestId, String action, String orderNo) {
        if (!StringUtils.hasText(requestId)) {
            throw new BusinessException(BizCode.BIZ_FAIL, "requestId 不能为空（业务端点必须带幂等键）");
        }
        String key = requestId.trim();
        WmsRequestLog exists = selectByRequestId(key);
        if (exists != null) {
            return replayOf(exists);
        }
        WmsRequestLog row = new WmsRequestLog();
        row.setRequestId(key);
        row.setAction(action);
        row.setOrderNo(orderNo == null ? "" : orderNo);
        row.setCreateTime(LocalDateTime.now());
        try {
            requestLogMapper.insert(row);
        } catch (DuplicateKeyException e) {
            WmsRequestLog racing = selectByRequestId(key);
            if (racing != null) {
                return replayOf(racing);
            }
            throw new BusinessException(BizCode.BIZ_FAIL, "该请求正在处理中，请稍后刷新");
        }
        return null;
    }

    @Override
    public void finish(String requestId, Map<String, Object> result) {
        if (!StringUtils.hasText(requestId) || result == null) {
            return;
        }
        String json;
        try {
            json = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            // 快照序列化失败不该把已经成功的业务打回去
            log.warn("幂等结果序列化失败，requestId={}", requestId, e);
            return;
        }
        requestLogMapper.update(null, new LambdaUpdateWrapper<WmsRequestLog>()
                .eq(WmsRequestLog::getRequestId, requestId.trim())
                .set(WmsRequestLog::getResultJson, json));
    }

    private Map<String, Object> replayOf(WmsRequestLog row) {
        if (!StringUtils.hasText(row.getResultJson())) {
            throw new BusinessException(BizCode.BIZ_FAIL, "该请求正在处理中，请稍后刷新");
        }
        try {
            Map<String, Object> map = objectMapper.readValue(row.getResultJson(), MAP_TYPE);
            map.put("replayed", true);
            return map;
        } catch (Exception e) {
            throw new BusinessException(BizCode.BIZ_FAIL, "上次执行结果无法解析，请重新提交");
        }
    }

    private WmsRequestLog selectByRequestId(String requestId) {
        return requestLogMapper.selectOne(new LambdaQueryWrapper<WmsRequestLog>()
                .eq(WmsRequestLog::getRequestId, requestId)
                .last("LIMIT 1"));
    }
}
