package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 商品SKU表 VO
 */
@Data
public class WmsSkuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private String skuCode;
    private String skuName;
    private String barcode;
    private Long categoryId;
    private String spec;
    private String unit;
    private BigDecimal weight;
    private BigDecimal volume;
    private Integer shelfLife;
    private Integer isBatch;
    private Integer isSerial;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
