package com.wms_warehouse.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 入库单明细表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_inbound_order_item")
public class WmsInboundOrderItem extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 入库单ID
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
     * 预期数量
     */
    private BigDecimal expectedQty;

    /**
     * 实收数量
     */
    private BigDecimal receivedQty;

    /**
     * 已上架数量
     */
    private BigDecimal putawayQty;

    /**
     * 指定上架库位(可空)
     */
    private Long locationId;

    /**
     * 状态:PENDING/PARTIAL/DONE
     */
    private String status;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
