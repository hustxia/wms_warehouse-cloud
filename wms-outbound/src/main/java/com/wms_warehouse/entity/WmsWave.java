package com.wms_warehouse.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 波次表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_wave")
public class WmsWave extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 波次号
     */
    private String waveNo;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 类型:按订单/按路线/按承运商
     */
    private String waveType;

    /**
     * 订单数量
     */
    private Integer orderCount;

    /**
     * 状态:CREATED/RELEASED/PICKING/DONE
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
