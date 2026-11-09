package com.wms_warehouse.common.result;

/**
 * 业务错误码（WMS V1.0 业务闭环）。
 *
 * <p>沿用项目既有约定：HTTP 恒为 200，业务结果看 {@code body.code}。
 * 与 {@link ResultCode} 分开是为了让「业务校验失败」和「服务异常」在前端可区分：
 * 前者提示用户改操作，后者提示联系管理员。
 */
public final class BizCode {

    private BizCode() {
    }

    /** 单据/明细/库存不存在 */
    public static final int NOT_FOUND = 40400;

    /** 业务校验失败：状态不允许、数量不合法、库存不足、重复请求处理中 */
    public static final int BIZ_FAIL = 40000;

    /** V1.1：通用 CRUD 携带了只能由业务端点写入的字段（数量/状态/承运信息） */
    public static final int FIELD_READONLY = 40001;
}
