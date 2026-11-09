package com.wms_warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wms_warehouse.client.InventoryClient;
import com.wms_warehouse.client.MasterClient;
import com.wms_warehouse.common.api.InventoryIncreaseRequest;
import com.wms_warehouse.common.api.InventoryKey;
import com.wms_warehouse.common.api.LocationInfo;
import com.wms_warehouse.common.api.SkuInfo;
import com.wms_warehouse.common.context.UserContext;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.idempotent.WmsIdempotentService;
import com.wms_warehouse.common.result.BizCode;
import com.wms_warehouse.common.statemachine.DocStateMachine;
import com.wms_warehouse.common.util.QtyUtils;
import com.wms_warehouse.dto.biz.PutawayRequest;
import com.wms_warehouse.dto.biz.ReceiveRequest;
import com.wms_warehouse.entity.WmsInboundOrder;
import com.wms_warehouse.entity.WmsInboundOrderItem;
import com.wms_warehouse.entity.WmsPutawayTask;
import com.wms_warehouse.entity.WmsReceiptRecord;
import com.wms_warehouse.service.WmsInboundBizService;
import com.wms_warehouse.service.WmsInboundOrderItemService;
import com.wms_warehouse.service.WmsInboundOrderService;
import com.wms_warehouse.service.WmsPutawayTaskService;
import com.wms_warehouse.service.WmsReceiptRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 入库业务闭环实现（微服务版）。
 *
 * <p><b>事务边界</b>：本方法的事务只覆盖本服务的表（入库单/明细/收货记录/上架任务）；
 * 库存写入通过 {@link InventoryClient} 调用 wms-inventory，那是一次独立的远程提交。
 * 先后顺序是「先本地写、后远程调」：
 * <ul>
 *   <li>远程失败 → 抛异常 → 本地全部回滚，账面一致（最常见的情况）；</li>
 *   <li>远程成功但本地提交失败 → 会多出一笔库存。这是共库拆分的固有缺口，
 *       没有 XA/事务消息就消不掉；项目用「在库量 = 流水 qty_change 之和」这条恒等式对账发现，
 *       再用盘点或冲销单据修正。生产环境应换成事务消息或本地消息表。</li>
 * </ul>
 */
@Slf4j
@Service
public class WmsInboundBizServiceImpl implements WmsInboundBizService {

    private static final String ACTION_RECEIVE = "RECEIVE";
    private static final String ACTION_PUTAWAY = "PUTAWAY";

    @Autowired
    private WmsInboundOrderService inboundOrderService;

    @Autowired
    private WmsInboundOrderItemService inboundOrderItemService;

    @Autowired
    private WmsReceiptRecordService receiptRecordService;

    @Autowired
    private WmsPutawayTaskService putawayTaskService;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private MasterClient masterClient;

    @Autowired
    private WmsIdempotentService idempotentService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> receive(Long orderId, ReceiveRequest request) {
        WmsInboundOrder order = requireOrder(orderId);
        Map<String, Object> replay = idempotentService.beginOrReplay(
                request.getRequestId(), ACTION_RECEIVE, order.getOrderNo());
        if (replay != null) {
            return replay;
        }
        DocStateMachine.require(DocStateMachine.INBOUND, order.getStatus(), "收货",
                "CREATED", "IN_TRANSIT", "RECEIVING");

        boolean hadReceiving = "RECEIVING".equals(order.getStatus());
        Map<Long, WmsInboundOrderItem> items = loadItems(orderId);
        List<Map<String, Object>> itemResults = new ArrayList<>();
        Long operatorId = UserContext.getUserId();

        for (ReceiveRequest.Item reqItem : request.getItems()) {
            WmsInboundOrderItem item = items.get(reqItem.getItemId());
            if (item == null) {
                throw new BusinessException(BizCode.NOT_FOUND, "入库单明细不存在：" + reqItem.getItemId());
            }
            BigDecimal qty = QtyUtils.requirePositive(reqItem.getReceivedQty(), "收货数量");
            BigDecimal expected = QtyUtils.nz(item.getExpectedQty());
            BigDecimal received = QtyUtils.nz(item.getReceivedQty()).add(qty);
            if (received.compareTo(expected) > 0) {
                throw new BusinessException(BizCode.BIZ_FAIL,
                        "第 " + item.getLineNo() + " 行收货数量超过预期（已收 "
                                + QtyUtils.plain(item.getReceivedQty()) + "，预期 " + QtyUtils.plain(expected) + "）");
            }
            item.setReceivedQty(received);
            item.setStatus(received.compareTo(expected) >= 0 ? "DONE" : "PARTIAL");
            // 计划批次为空时才接受实收批次；单据已定批次就不能在这里改
            if (!StringUtils.hasText(item.getBatchNo()) && StringUtils.hasText(reqItem.getBatchNo())) {
                item.setBatchNo(reqItem.getBatchNo().trim());
            }
            inboundOrderItemService.updateById(item);

            WmsReceiptRecord record = new WmsReceiptRecord();
            record.setOrderId(orderId);
            record.setItemId(item.getId());
            record.setSkuId(item.getSkuId());
            record.setBatchNo(item.getBatchNo() == null ? "" : item.getBatchNo());
            record.setReceivedQty(qty);
            record.setOperatorId(operatorId);
            record.setReceivedAt(LocalDateTime.now());
            receiptRecordService.save(record);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", String.valueOf(item.getId()));
            row.put("receivedQty", received);
            row.put("status", item.getStatus());
            itemResults.add(row);
        }

        boolean allDone = items.values().stream().allMatch(i -> "DONE".equals(i.getStatus()));
        String next = allDone ? "RECEIVED" : "RECEIVING";
        // 一次性收全也走 CREATED→RECEIVING→RECEIVED 两跳：状态轨迹要能还原「收过货」
        if (allDone && !hadReceiving) {
            DocStateMachine.move(DocStateMachine.INBOUND, order.getStatus(), "RECEIVING");
        }
        DocStateMachine.move(DocStateMachine.INBOUND, allDone ? "RECEIVING" : order.getStatus(), next);
        order.setStatus(next);
        inboundOrderService.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", next);
        result.put("items", itemResults);
        idempotentService.finish(request.getRequestId(), result);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> putaway(Long orderId, PutawayRequest request) {
        WmsInboundOrder order = requireOrder(orderId);
        Map<String, Object> replay = idempotentService.beginOrReplay(
                request.getRequestId(), ACTION_PUTAWAY, order.getOrderNo());
        if (replay != null) {
            return replay;
        }
        // 没收货就上架＝凭空造库存，必须拦在状态机这一层
        DocStateMachine.require(DocStateMachine.INBOUND, order.getStatus(), "上架", "RECEIVED");

        Map<Long, WmsInboundOrderItem> items = loadItems(orderId);
        BigDecimal totalPutaway = BigDecimal.ZERO;
        Map<Long, SkuInfo> skuCache = new LinkedHashMap<>();

        for (PutawayRequest.Item reqItem : request.getItems()) {
            WmsInboundOrderItem item = items.get(reqItem.getItemId());
            if (item == null) {
                throw new BusinessException(BizCode.NOT_FOUND, "入库单明细不存在：" + reqItem.getItemId());
            }
            BigDecimal qty = QtyUtils.requirePositive(reqItem.getPutawayQty(), "上架数量");
            BigDecimal received = QtyUtils.nz(item.getReceivedQty());
            BigDecimal putaway = QtyUtils.nz(item.getPutawayQty());
            if (putaway.add(qty).compareTo(received) > 0) {
                throw new BusinessException(BizCode.BIZ_FAIL,
                        "第 " + item.getLineNo() + " 行上架数量不能大于已收数量（已收 "
                                + QtyUtils.plain(received) + "，已上架 " + QtyUtils.plain(putaway) + "）");
            }

            LocationInfo location = masterClient.requireLocation(reqItem.getLocationId());
            if (location.getWarehouseId() != null && !location.getWarehouseId().equals(order.getWarehouseId())) {
                throw new BusinessException(BizCode.BIZ_FAIL,
                        "库位 " + location.getLocationCode() + " 不属于该入库单的仓库");
            }

            InventoryKey key = buildKey(order, item, reqItem.getLocationId(), skuCache);
            InventoryIncreaseRequest increase = new InventoryIncreaseRequest();
            increase.setKey(key);
            increase.setQty(qty);
            increase.setTxnType("PUTAWAY");
            increase.setRefNo(order.getOrderNo());
            increase.setRemark("入库上架，库位 " + location.getLocationCode());
            inventoryClient.increaseOrThrow(increase);

            item.setPutawayQty(putaway.add(qty));
            item.setLocationId(reqItem.getLocationId());
            inboundOrderItemService.updateById(item);

            WmsPutawayTask task = new WmsPutawayTask();
            task.setTaskNo(QtyUtils.nextNo("PT"));
            task.setOrderId(orderId);
            task.setItemId(item.getId());
            task.setSkuId(item.getSkuId());
            task.setToLocation(reqItem.getLocationId());
            task.setBatchNo(item.getBatchNo() == null ? "" : item.getBatchNo());
            task.setTaskQty(qty);
            task.setDoneQty(qty);
            task.setStatus("DONE");
            task.setAssigneeId(UserContext.getUserId());
            task.setCreatedAt(LocalDateTime.now());
            task.setFinishedAt(LocalDateTime.now());
            putawayTaskService.save(task);

            totalPutaway = totalPutaway.add(qty);
        }

        boolean allPutaway = items.values().stream()
                .allMatch(i -> QtyUtils.nz(i.getPutawayQty()).compareTo(QtyUtils.nz(i.getReceivedQty())) >= 0);
        if (allPutaway) {
            DocStateMachine.move(DocStateMachine.INBOUND, order.getStatus(), "PUTAWAY_DONE");
            order.setStatus("PUTAWAY_DONE");
            inboundOrderService.updateById(order);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus());
        result.put("putawayQty", totalPutaway);
        idempotentService.finish(request.getRequestId(), result);
        return result;
    }

    /**
     * 组装库存业务键。
     *
     * <p>批次管理 + 有保质期的 SKU，上架时按「今天生产、今天+保质期到期」估算效期。
     * 入库单明细没有生产日期/到期日列（收货时的批次日期无处落库），而 FEFO 依赖效期排序，
     * 没有效期的批次会永远排最后。真实系统应在收货单上登记批次效期，本版先按保质期推算，
     * V1.1 给明细表加两列即可替换这里。
     *
     * <p>SKU 来自 wms-master 的内部接口（带本地缓存，一单多行只查一次）。
     */
    private InventoryKey buildKey(WmsInboundOrder order, WmsInboundOrderItem item,
                                  Long locationId, Map<Long, SkuInfo> skuCache) {
        String batchNo = item.getBatchNo() == null ? "" : item.getBatchNo();
        LocalDate productionDate = null;
        LocalDate expiryDate = null;
        if (StringUtils.hasText(batchNo)) {
            SkuInfo sku = skuCache.computeIfAbsent(item.getSkuId(), masterClient::findSku);
            if (sku != null && Integer.valueOf(1).equals(sku.getIsBatch()) && sku.getShelfLife() != null) {
                productionDate = LocalDate.now();
                expiryDate = productionDate.plusDays(sku.getShelfLife());
            }
        }
        return new InventoryKey(order.getWarehouseId(), order.getOwnerId(), item.getSkuId(),
                locationId, batchNo, productionDate, expiryDate);
    }

    private WmsInboundOrder requireOrder(Long orderId) {
        WmsInboundOrder order = inboundOrderService.getById(orderId);
        if (order == null) {
            throw new BusinessException(BizCode.NOT_FOUND, "入库单不存在");
        }
        return order;
    }

    /** 一次性载入明细并按 id 建索引：逐行 getById 会打出 N 次 SQL，行多了就是灾难 */
    private Map<Long, WmsInboundOrderItem> loadItems(Long orderId) {
        List<WmsInboundOrderItem> list = inboundOrderItemService.list(
                new LambdaQueryWrapper<WmsInboundOrderItem>()
                        .eq(WmsInboundOrderItem::getOrderId, orderId)
                        .orderByAsc(WmsInboundOrderItem::getLineNo));
        if (list.isEmpty()) {
            throw new BusinessException(BizCode.BIZ_FAIL, "入库单没有明细行");
        }
        return list.stream().collect(Collectors.toMap(WmsInboundOrderItem::getId, Function.identity(),
                (a, b) -> a, LinkedHashMap::new));
    }
}
