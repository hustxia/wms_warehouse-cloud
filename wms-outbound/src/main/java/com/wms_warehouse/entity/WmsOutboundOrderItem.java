package com.wms_warehouse.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 出库单明细表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_outbound_order_item")
public class WmsOutboundOrderItem extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 出库单ID
     */
    private Long orderId;

    /**
     * 行号
     */
    private Integer lineNo;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 订单数量
     */
    private BigDecimal orderQty;

    /**
     * 已分配数量
     */
    private BigDecimal allocatedQty;

    /**
     * 已拣数量
     */
    private BigDecimal pickedQty;

    /**
     * 已发数量
     */
    private BigDecimal shippedQty;

    /**
     * 状态:PENDING/PARTIAL/DONE
     */
    private String status;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
