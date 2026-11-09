package com.wms_warehouse.dto.biz;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发运请求（POST /wms_outbound_order/{id}/ship）。
 */
@Data
public class ShipRequest {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    @NotBlank(message = "承运商不能为空")
    private String carrier;

    @NotBlank(message = "运单号不能为空")
    private String trackingNo;
}
