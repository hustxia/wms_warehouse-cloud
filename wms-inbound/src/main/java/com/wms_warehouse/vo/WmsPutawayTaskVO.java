package com.wms_warehouse.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 上架任务表 VO
 */
@Data
public class WmsPutawayTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="vo-fields">
    private Long id;
    private String taskNo;
    private Long orderId;
    private Long itemId;
    private Long skuId;
    private Long fromLocation;
    private Long toLocation;
    private String batchNo;
    private BigDecimal taskQty;
    private BigDecimal doneQty;
    private String status;
    private Long assigneeId;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // <gen:end key="vo-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
