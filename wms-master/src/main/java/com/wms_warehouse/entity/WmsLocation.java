package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库位表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_location")
public class WmsLocation extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 所属仓库ID
     */
    private Long warehouseId;

    /**
     * 所属库区ID
     */
    private Long zoneId;

    /**
     * 库位编码
     */
    private String locationCode;

    /**
     * 类型:横梁/流利/地堆/冷藏
     */
    private String locationType;

    /**
     * 排
     */
    private Integer rowNo;

    /**
     * 列
     */
    private Integer colNo;

    /**
     * 层
     */
    private Integer layerNo;

    /**
     * 最大容量
     */
    private BigDecimal maxCapacity;

    /**
     * 是否允许混放:1是 0否
     */
    private Integer isMixed;

    /**
     * 状态:1可用 0锁定
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
