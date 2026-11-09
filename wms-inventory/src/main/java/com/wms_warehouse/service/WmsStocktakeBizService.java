package com.wms_warehouse.service;

import com.wms_warehouse.dto.biz.CountRequest;
import com.wms_warehouse.dto.biz.StocktakeCreateRequest;

import java.util.Map;

/**
 * 盘点业务闭环（P0-4）：创建快照 → 冻结 → 录实盘 → 确认调整。
 *
 * <p>关键口径：差异以**创建时的账面快照**为基准，不是以确认时的实时库存为基准。
 * 否则盘点期间若发生其他变动，差异会被算错，盘出来的是「一笔糊涂账」。
 */
public interface WmsStocktakeBizService {

    /** 创建盘点单并按范围快照生成明细（system_qty = 当前在库量） */
    Map<String, Object> create(StocktakeCreateRequest request);

    /** 冻结：把每条明细对应库存的可用量锁住，冻结期间不可被出库分配 */
    Map<String, Object> freeze(Long stocktakeId, String requestId);

    /** 录实盘数量 */
    Map<String, Object> count(Long itemId, CountRequest request);

    /** 确认调整：按差异调整在库量，写 ADJUST 流水，释放冻结 */
    Map<String, Object> confirm(Long stocktakeId, String requestId);
}
