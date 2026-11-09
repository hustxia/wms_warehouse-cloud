package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 入库单主表 VO
 */
@Data
public class WmsInboundOrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private String orderNo;
    private String orderType;
    private Long warehouseId;
    private Long ownerId;
    private Long supplierId;
    private String refNo;
    private String status;
    private LocalDate expectedDate;
    private Long operatorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
