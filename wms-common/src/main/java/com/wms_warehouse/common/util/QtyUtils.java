package com.wms_warehouse.common.util;

import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.BizCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数量与单号工具（WMS V1.0）。
 *
 * <p>数量统一 {@code DECIMAL(14,3)}：比较一律走 {@link BigDecimal#compareTo}，
 * 不用 {@code equals}——{@code 1.0} 与 {@code 1.000} 在 equals 下不相等，是库存校验的经典坑。
 */
public final class QtyUtils {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final AtomicInteger SEQ = new AtomicInteger(ThreadLocalRandom.current().nextInt(100, 900));

    private QtyUtils() {
    }

    /** null 视为 0，避免满屏 null 判断 */
    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 校验「必须大于 0」，并把小数位收敛到 3 位（与列精度一致，避免写入时被静默截断）。
     */
    public static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (!positive(value)) {
            throw new BusinessException(BizCode.BIZ_FAIL, fieldName + "必须大于 0");
        }
        return value.setScale(3, RoundingMode.DOWN);
    }

    /** 展示用：去掉无意义的尾零（6.000 → 6），错误提示里不出现 6.000 这种读起来费劲的数字 */
    public static String plain(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        BigDecimal stripped = value.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }

    /**
     * 业务单号：前缀 + 毫秒时间戳 + 3 位序号。
     *
     * <p>不用 UUID：单号要人能读、能按时间排序。3 位序号由进程内自增提供，
     * 同一毫秒内并发也不会撞（唯一键兜底，撞了会抛，不会静默覆盖）。
     */
    public static String nextNo(String prefix) {
        int seq = Math.abs(SEQ.getAndIncrement() % 1000);
        return prefix + LocalDateTime.now().format(TS) + String.format("%03d", seq);
    }
}
