package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 收货记录表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_receipt_record")
public class WmsReceiptRecord extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 入库单ID
     */
    private Long orderId;

    /**
     * 入库单明细ID
     */
    private Long itemId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 收货数量
     */
    private BigDecimal receivedQty;

    /**
     * 暂存库位
     */
    private Long locationId;

    /**
     * 操作人
     */
    private Long operatorId;

    /**
     * 收货时间
     */
    private LocalDateTime receivedAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
