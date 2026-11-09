package com.wms_warehouse.service;

import com.wms_warehouse.dto.biz.PutawayRequest;
import com.wms_warehouse.dto.biz.ReceiveRequest;

import java.util.Map;

/**
 * 入库业务闭环（P0-2）：收货 → 上架。
 *
 * <p>业务端点与 CRUD 端点分开：CRUD 是主数据维护（改单、改地址），业务端点是事实记录（收了多少、放哪了），
 * 后者一旦发生就不能靠「编辑」改回来，只能靠反向单据。混在一个 Service 里迟早会被前端误用。
 */
public interface WmsInboundBizService {

    /** 收货登记：累加明细实收数量，写收货记录，推进单据状态 */
    Map<String, Object> receive(Long orderId, ReceiveRequest request);

    /** 上架：按库位增加库存并写流水，明细已上架数量累加，全部上架后单据完成 */
    Map<String, Object> putaway(Long orderId, PutawayRequest request);
}
