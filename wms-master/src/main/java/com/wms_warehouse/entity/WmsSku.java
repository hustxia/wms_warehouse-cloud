package com.wms_warehouse.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wms_warehouse.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品SKU表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_sku")
public class WmsSku extends BaseEntity {

    // <gen:begin key="entity-fields">
    /**
     * SKU编码
     */
    private String skuCode;

    /**
     * 商品名称
     */
    private String skuName;

    /**
     * 条码
     */
    private String barcode;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 规格
     */
    private String spec;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 重量(kg)
     */
    private BigDecimal weight;

    /**
     * 体积(m³)
     */
    private BigDecimal volume;

    /**
     * 保质期(天)
     */
    private Integer shelfLife;

    /**
     * 是否批次管理:1是 0否
     */
    private Integer isBatch;

    /**
     * 是否序列号管理:1是 0否
     */
    private Integer isSerial;

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
