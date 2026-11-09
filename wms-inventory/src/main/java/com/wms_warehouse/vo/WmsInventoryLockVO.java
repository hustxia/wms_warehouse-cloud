package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 库存锁定表 VO
 */
@Data
public class WmsInventoryLockVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private String lockNo;
    private Long inventoryId;
    private Long skuId;
    private Long locationId;
    private BigDecimal lockedQty;
    private String refOrderNo;
    private String lockType;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime releasedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
