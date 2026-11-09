package com.wms_warehouse.common.statemachine;

import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 单据状态机（WMS V1.0）。
 *
 * <p>状态与允许的转移集中在这里，业务代码只做两件事：
 * 用 {@link #require} 声明「这个动作要求当前是什么状态」，用 {@link #move} 声明「它要把单据推到哪」。
 * 不在 Service 里散写 {@code if ("CREATED".equals(status))}——那种写法出现第三张单据就失控了。
 *
 * <p>只允许相邻状态推进，跳级（如入库单 CREATED 直接上架）在 {@link #move} 就会被拒绝。
 */
public final class DocStateMachine {

    public static final String INBOUND = "INBOUND";
    public static final String OUTBOUND = "OUTBOUND";
    public static final String STOCKTAKE = "STOCKTAKE";

    /** doc -> (from -> 允许的 to) */
    private static final Map<String, Map<String, Set<String>>> TRANSITIONS = new HashMap<>();

    /** 状态中文名，错误提示直接给中文，前端不必再维护一份映射 */
    private static final Map<String, String> LABELS = new HashMap<>();

    static {
        TRANSITIONS.put(INBOUND, Map.of(
                "CREATED", Set.of("IN_TRANSIT", "RECEIVING", "CANCELLED"),
                "IN_TRANSIT", Set.of("RECEIVING", "CANCELLED"),
                "RECEIVING", Set.of("RECEIVED"),
                "RECEIVED", Set.of("PUTAWAY_DONE")
        ));
        TRANSITIONS.put(OUTBOUND, Map.of(
                "CREATED", Set.of("ALLOCATED", "PARTIAL", "CANCELLED"),
                "PARTIAL", Set.of("ALLOCATED", "PICKING", "CREATED", "CANCELLED"),
                "ALLOCATED", Set.of("PICKING", "PICKED", "CREATED", "CANCELLED"),
                "PICKING", Set.of("PICKED"),
                "PICKED", Set.of("SHIPPED")
        ));
        TRANSITIONS.put(STOCKTAKE, Map.of(
                "CREATED", Set.of("FROZEN", "CANCELLED"),
                "FROZEN", Set.of("CONFIRMED")
        ));

        LABELS.put("CREATED", "已创建");
        LABELS.put("IN_TRANSIT", "在途");
        LABELS.put("RECEIVING", "收货中");
        LABELS.put("RECEIVED", "已收货");
        LABELS.put("PUTAWAY_DONE", "已上架");
        LABELS.put("ALLOCATED", "已分配");
        LABELS.put("PARTIAL", "部分满足");
        LABELS.put("PICKING", "拣货中");
        LABELS.put("PICKED", "已拣货");
        LABELS.put("SHIPPED", "已发运");
        LABELS.put("FROZEN", "已冻结");
        LABELS.put("CONFIRMED", "已确认");
        LABELS.put("COUNTING", "盘点中");
        LABELS.put("CANCELLED", "已取消");
    }

    private DocStateMachine() {
    }

    public static String label(String status) {
        return status == null ? "未知" : LABELS.getOrDefault(status, status);
    }

    /**
     * 声明动作的准入状态。当前状态不在允许集合内直接抛业务异常。
     *
     * @param doc     单据类型（{@link #INBOUND} 等），仅用于日志语义
     * @param current 当前状态
     * @param action  动作中文名，用于提示「当前状态「已发货」不允许<b>拣货</b>」
     * @param allowed 允许的当前状态
     */
    public static void require(String doc, String current, String action, String... allowed) {
        for (String status : allowed) {
            if (status.equals(current)) {
                return;
            }
        }
        throw new BusinessException(BizCode.BIZ_FAIL,
                "当前状态「" + label(current) + "」不允许" + action);
    }

    /**
     * 校验一次状态推进是否合法（防跳级）。
     *
     * <p>同一状态不视为流转（幂等写入时常见），直接放行。
     */
    public static void move(String doc, String from, String to) {
        if (from == null || from.equals(to)) {
            return;
        }
        Set<String> next = TRANSITIONS.getOrDefault(doc, Map.of()).getOrDefault(from, Set.of());
        if (!next.contains(to)) {
            throw new BusinessException(BizCode.BIZ_FAIL,
                    "非法状态流转：" + label(from) + " → " + label(to));
        }
    }
}
