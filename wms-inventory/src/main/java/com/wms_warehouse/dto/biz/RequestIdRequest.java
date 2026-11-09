package com.wms_warehouse.dto.biz;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 只带幂等键的请求体：冻结、确认调整这类「一键到底」的动作不需要其他参数。
 */
@Data
public class RequestIdRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;
}
