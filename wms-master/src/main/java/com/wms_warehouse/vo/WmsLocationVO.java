package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 库位表 VO
 */
@Data
public class WmsLocationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private Long warehouseId;
    private Long zoneId;
    private String locationCode;
    private String locationType;
    private Integer rowNo;
    private Integer colNo;
    private Integer layerNo;
    private BigDecimal maxCapacity;
    private Integer isMixed;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
