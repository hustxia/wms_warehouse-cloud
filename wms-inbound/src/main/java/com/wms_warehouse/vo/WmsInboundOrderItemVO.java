package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 入库单明细表 VO
 */
@Data
public class WmsInboundOrderItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private Long orderId;
    private Integer lineNo;
    private Long skuId;
    private String batchNo;
    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
    private BigDecimal putawayQty;
    private Long locationId;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
