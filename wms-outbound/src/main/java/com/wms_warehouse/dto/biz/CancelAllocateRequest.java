package com.wms_warehouse.dto.biz;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 取消分配请求（POST /wms_outbound_order/{id}/cancel-allocate）。
 */
@Data
public class CancelAllocateRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;
}
