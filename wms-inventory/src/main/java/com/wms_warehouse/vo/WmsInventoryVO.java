package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存表 VO
 */
@Data
public class WmsInventoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private Long warehouseId;
    private Long ownerId;
    private Long skuId;
    private Long locationId;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal qtyOnHand;
    private BigDecimal qtyAllocated;
    private BigDecimal qtyLocked;
    private BigDecimal qtyAvailable;
    private Integer version;
    private LocalDateTime updatedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
