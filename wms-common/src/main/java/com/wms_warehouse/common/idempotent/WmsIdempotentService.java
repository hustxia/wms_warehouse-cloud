package com.wms_warehouse.common.idempotent;

import java.util.Map;

/**
 * 业务端点幂等（wms_request_log）。
 *
 * <p>为什么不是「查单据状态」做幂等：状态机只能挡住「已发运再发运」，
 * 挡不住同一状态下的重复提交——连点两次拣货，两次都过状态校验，库存会被扣两遍。
 *
 * <p>用法（必须与业务逻辑在同一个本地事务里）：
 * <pre>
 * Map&lt;String,Object&gt; replay = idempotent.beginOrReplay(requestId, "PICK", orderNo);
 * if (replay != null) return replay;
 * ... 业务处理 ...
 * idempotent.finish(requestId, result);
 * </pre>
 * 业务抛异常时事务回滚，占位行一起回滚，用户重试仍能执行——不会「失败一次永久占键」。
 */
public interface WmsIdempotentService {

    /**
     * 占位并判断是否重复提交。
     *
     * @return {@code null} 表示首次执行；非 null 表示重复提交，直接返回该结果
     */
    Map<String, Object> beginOrReplay(String requestId, String action, String orderNo);

    /** 记录首次执行结果，供后续重复请求原样返回 */
    void finish(String requestId, Map<String, Object> result);
}
