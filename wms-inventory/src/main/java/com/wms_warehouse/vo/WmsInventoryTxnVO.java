package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 库存事务流水表 VO
 */
@Data
public class WmsInventoryTxnVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private String txnNo;
    private String txnType;
    private Long warehouseId;
    private Long ownerId;
    private Long skuId;
    private Long locationId;
    private String batchNo;
    private BigDecimal qtyChange;
    private BigDecimal qtyBefore;
    private BigDecimal qtyAfter;
    private String refOrderNo;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
