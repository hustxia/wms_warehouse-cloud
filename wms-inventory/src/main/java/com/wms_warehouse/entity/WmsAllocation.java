package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存分配明细表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_allocation")
public class WmsAllocation extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 出库单ID
     */
    private Long orderId;

    /**
     * 出库单明细ID
     */
    private Long itemId;

    /**
     * 库存ID
     */
    private Long inventoryId;

    /**
     * SKU ID
     */
    private Long skuId;

    /**
     * 库位ID
     */
    private Long locationId;

    /**
     * 批次号
     */
    private String batchNo;

    /**
     * 分配数量
     */
    private BigDecimal allocatedQty;

    /**
     * 状态:1有效 0已释放
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
