package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 拣货任务表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_pick_task")
public class WmsPickTask extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 拣货任务号
     */
    private String taskNo;

    /**
     * 出库单ID
     */
    private Long orderId;

    /**
     * 出库单明细ID
     */
    private Long itemId;

    /**
     * 关联分配明细ID
     */
    private Long allocationId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 拣货库位ID
     */
    private Long locationId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 任务数量
     */
    private BigDecimal taskQty;

    /**
     * 已拣数量
     */
    private BigDecimal pickedQty;

    /**
     * 波次号
     */
    private String waveNo;

    /**
     * 状态:PENDING/ASSIGNED/DOING/DONE/EXCEPTION
     */
    private String status;

    /**
     * 作业员
     */
    private Long assigneeId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 完成时间
     */
    private LocalDateTime finishedAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
