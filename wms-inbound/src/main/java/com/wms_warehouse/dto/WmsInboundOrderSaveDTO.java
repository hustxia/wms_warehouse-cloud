package com.wms_warehouse.dto;

import java.time.LocalDateTime;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 入库单主表保存 DTO（新增/修改）
 */
@Data
public class WmsInboundOrderSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // <gen:begin key="dto-fields">
    /**
     * 主键，新增时为空
     */
    private Long id;

    @NotBlank(message = "入库单号不能为空") @Size(max = 64, message = "入库单号长度不能超过64")
    private String orderNo;
    @NotBlank(message = "类型不能为空") @Size(max = 32, message = "类型长度不能超过32")
    private String orderType;
    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;
    @NotNull(message = "货主ID不能为空")
    private Long ownerId;
    private Long supplierId;
    @Size(max = 64, message = "外部单号长度不能超过64")
    private String refNo;
    // V1.1 F-03：状态由状态机/业务端点写入，通用 CRUD 不接受也不强制必填。
    // 去掉 @NotBlank 是必要的——否则前端按只读规则剔除 status 后会被 @Valid 拦住。
    @Size(max = 32, message = "状态长度不能超过32")
    private String status;
    private LocalDate expectedDate;
    private Long operatorId;
    // 审计时间由 DB 默认值 / AuditMetaObjectHandler 填充；MyBatis-Plus 默认 NOT_NULL
    // 策略下 null 字段不会进 INSERT/UPDATE，不会把库里的值写成 null（坑库 34）。
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // <gen:end key="dto-fields">
    // ===== 以下为手写区：写在 gen:end 之后的内容，重新生成不会被覆盖 =====
}
