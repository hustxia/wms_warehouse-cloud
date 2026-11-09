package com.wms_warehouse.common.idempotent;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务端点幂等键（wms_request_log）。
 *
 * <p>下沉到 wms-common 的原因：入库、出库、库存三个服务都要用它，
 * 而各服务共库同表——同一张幂等表放在公共模块，比每个服务各存一份实现更不容易走偏。
 *
 * <p>刻意不继承 {@code BaseEntity}：技术表，无 update_time/update_by/deleted，
 * 也不做逻辑删除（幂等键只需要「写入」与「回读」）。
 */
@Data
@TableName("wms_request_log")
public class WmsRequestLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户端幂等键（UUID），唯一键 uk_request */
    private String requestId;

    /** 动作：RECEIVE/PUTAWAY/ALLOCATE/PICK/SHIP/CANCEL_ALLOCATE/FREEZE/COUNT/STOCKTAKE_CONFIRM */
    private String action;

    /** 关联单据号 */
    private String orderNo;

    /** 首次执行结果快照；为 null 表示「已占位、尚未执行完」 */
    private String resultJson;

    private LocalDateTime createTime;
}
