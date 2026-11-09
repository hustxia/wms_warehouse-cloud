package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 盘点单表 VO
 */
@Data
public class WmsStocktakeOrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private String stocktakeNo;
    private Long warehouseId;
    private Long zoneId;
    private String stocktakeType;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
