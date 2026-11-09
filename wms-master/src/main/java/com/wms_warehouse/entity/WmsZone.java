package com.wms_warehouse.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库区表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_zone")
public class WmsZone extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * 所属仓库ID
     */
    private Long warehouseId;

    /**
     * 库区编码
     */
    private String zoneCode;

    /**
     * 库区名称
     */
    private String zoneName;

    /**
     * 类型:存储区/拣选区/收货区/发货区/退货区
     */
    private String zoneType;

    /**
     * 状态:1启用 0停用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // <gen:end key="entity-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
