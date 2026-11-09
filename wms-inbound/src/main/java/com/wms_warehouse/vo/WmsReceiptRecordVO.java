package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 收货记录表 VO
 */
@Data
public class WmsReceiptRecordVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private Long orderId;
    private Long itemId;
    private Long skuId;
    private String batchNo;
    private BigDecimal receivedQty;
    private Long locationId;
    private Long operatorId;
    private LocalDateTime receivedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
