package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 发货单表 VO
 */
@Data
public class WmsShipmentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private String shipmentNo;
    private Long orderId;
    private String carrier;
    private String trackingNo;
    private Integer packageCount;
    private BigDecimal totalWeight;
    private String status;
    private LocalDateTime shippedAt;
    private LocalDateTime createdAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
