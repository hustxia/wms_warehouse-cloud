package com.wms_warehouse.controller;

import com.wms_warehouse.common.api.AllocateItemRequest;
import com.wms_warehouse.common.api.AllocationSlice;
import com.wms_warehouse.common.api.FefoQuery;
import com.wms_warehouse.common.api.InventoryIncreaseRequest;
import com.wms_warehouse.common.api.InventoryOpRequest;
import com.wms_warehouse.common.api.InventorySnapshot;
import com.wms_warehouse.common.api.PickItemRequest;
import com.wms_warehouse.common.exception.BusinessException;
import com.wms_warehouse.common.result.Result;
import com.wms_warehouse.common.result.ResultCode;
import com.wms_warehouse.common.util.QtyUtils;
import com.wms_warehouse.dto.biz.ReconcileAdjustRequest;
import com.wms_warehouse.entity.WmsInventory;
import com.wms_warehouse.mapper.WmsInventoryMapper;
import com.wms_warehouse.service.InventoryAllocationService;
import com.wms_warehouse.service.InventoryCoreService;
import com.wms_warehouse.service.InventoryReconcileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 库存服务的内部接口（服务间调用，不对网关暴露）。
 *
 * <p>为什么不是「别的服务直连库存表」：那等于把「库存唯一入口」这条硬规则交给自觉性。
 * 微服务拆分后，库存的写入口只有本服务这一个进程，入库/出库要改库存必须过这里，
 * 规则从「约定」变成「拓扑上做不到」。
 *
 * <p>安全边界：{@code /internal/**} 不在网关路由表里，只在内网可达；
 * 入口再校验 {@code X-Internal-Token}（配置项 {@code app.internal.token}），
 * 防止同网段的其他服务误调/乱调。生产环境应换成服务网格 mTLS 或签名鉴权。
 */
@Tag(name = "库存内部接口", description = "仅供服务间调用，网关不路由")
@RestController
@RequestMapping("/internal/inventory")
public class InternalInventoryController {

    private static final String TOKEN_HEADER = "X-Internal-Token";

    // V1.1 F-09：不提供默认值。缺配置就启动失败，好过带着公开仓库里的令牌悄悄跑起来。
    @Value("${app.internal.token}")
    private String internalToken;

    @Autowired
    private InventoryCoreService inventoryCoreService;

    @Autowired
    private InventoryReconcileService reconcileService;

    @Autowired
    private InventoryAllocationService allocationService;

    @Autowired
    private WmsInventoryMapper inventoryMapper;

    @Operation(summary = "增加在库量（上架/盘盈）")
    @PostMapping("/increase")
    public Result<Void> increase(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                 @RequestBody InventoryIncreaseRequest request) {
        checkToken(token);
        inventoryCoreService.increaseOnHand(request.getKey(), request.getQty(),
                request.getTxnType(), request.getRefNo(), request.getRemark());
        return Result.ok();
    }

    @Operation(summary = "减少在库量（拣货/盘亏）")
    @PostMapping("/decrease")
    public Result<Void> decrease(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                 @RequestBody InventoryOpRequest request) {
        checkToken(token);
        inventoryCoreService.decreaseOnHand(request.getInventoryId(), request.getQty(),
                request.getTxnType(), request.getRefNo(), request.getRemark());
        return Result.ok();
    }

    /**
     * 尝试分配。返回 false 表示可用量不足——调用方（出库服务）要继续试下一条候选库存，
     * 所以这里**不能**用异常表达「没抢到」。
     */
    @Operation(summary = "尝试分配占用（返回 false 表示可用量不足）")
    @PostMapping("/allocate")
    public Result<Boolean> allocate(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                    @RequestBody InventoryOpRequest request) {
        checkToken(token);
        return Result.ok(inventoryCoreService.tryAllocate(request.getInventoryId(), request.getQty(),
                request.getRefNo()));
    }

    @Operation(summary = "释放分配占用")
    @PostMapping("/release-allocate")
    public Result<Void> releaseAllocate(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                        @RequestBody InventoryOpRequest request) {
        checkToken(token);
        inventoryCoreService.releaseAllocate(request.getInventoryId(), request.getQty(), request.getRefNo());
        return Result.ok();
    }

    @Operation(summary = "冻结")
    @PostMapping("/lock")
    public Result<Void> lock(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                             @RequestBody InventoryOpRequest request) {
        checkToken(token);
        inventoryCoreService.lock(request.getInventoryId(), request.getQty(),
                StringUtils.hasText(request.getLockType()) ? request.getLockType() : "TRANSFER", request.getRefNo());
        return Result.ok();
    }

    @Operation(summary = "解冻")
    @PostMapping("/unlock")
    public Result<Void> unlock(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                               @RequestBody InventoryOpRequest request) {
        checkToken(token);
        inventoryCoreService.unlock(request.getInventoryId(), request.getQty(),
                StringUtils.hasText(request.getLockType()) ? request.getLockType() : "TRANSFER", request.getRefNo());
        return Result.ok();
    }

    @Operation(summary = "FEFO 候选库存（按效期先进先出排序）")
    @PostMapping("/fefo-candidates")
    public Result<List<InventorySnapshot>> fefoCandidates(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                                         @RequestBody FefoQuery query) {
        checkToken(token);
        List<InventorySnapshot> list = inventoryMapper
                .selectAvailableForFefo(query.getWarehouseId(), query.getOwnerId(), query.getSkuId())
                .stream().map(InternalInventoryController::toSnapshot).toList();
        return Result.ok(list);
    }

    @Operation(summary = "分配单行（FEFO，能分多少分多少）")
    @PostMapping("/allocate-item")
    public Result<List<AllocationSlice>> allocateItem(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                                      @RequestBody AllocateItemRequest request) {
        checkToken(token);
        return Result.ok(allocationService.allocate(request));
    }

    @Operation(summary = "拣货单行（扣在库量 + 释放占用，返回实际拣货切片）")
    @PostMapping("/pick-item")
    public Result<List<AllocationSlice>> pickItem(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                                  @RequestBody PickItemRequest request) {
        checkToken(token);
        return Result.ok(allocationService.pick(request));
    }

    @Operation(summary = "释放整单占用（取消分配）")
    @PostMapping("/release-order")
    public Result<Integer> releaseOrder(@RequestHeader(value = TOKEN_HEADER, required = false) String token,
                                        @RequestBody PickItemRequest request) {
        checkToken(token);
        return Result.ok(allocationService.releaseOrder(request.getOrderId(), request.getOrderNo()));
    }

    /**
     * V1.1 F-08：对账冲销。由运维脚本直连（网关不路由 /internal/**），
     * 把账面在库量校正到目标值，并写一条 ADJUST 流水，使最新流水重新与账面吻合。
     */
    @Operation(summary = "对账冲销（幂等，写 ADJUST 流水）")
    @PostMapping("/reconcile-adjust")
    public Result<Map<String, Object>> reconcileAdjust(
            @RequestHeader(value = TOKEN_HEADER, required = false) String token,
            @Valid @RequestBody ReconcileAdjustRequest request) {
        checkToken(token);
        return Result.ok(reconcileService.adjust(request));
    }

    private static InventorySnapshot toSnapshot(WmsInventory inv) {        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.setId(inv.getId());
        snapshot.setLocationId(inv.getLocationId());
        snapshot.setBatchNo(inv.getBatchNo());
        snapshot.setExpiryDate(inv.getExpiryDate());
        snapshot.setQtyOnHand(inv.getQtyOnHand());
        snapshot.setQtyAllocated(inv.getQtyAllocated());
        snapshot.setQtyLocked(inv.getQtyLocked());
        snapshot.setAvailable(QtyUtils.nz(inv.getQtyOnHand())
                .subtract(QtyUtils.nz(inv.getQtyAllocated()))
                .subtract(QtyUtils.nz(inv.getQtyLocked())));
        return snapshot;
    }

    private void checkToken(String token) {
        if (!StringUtils.hasText(token) || !token.equals(internalToken)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "内部调用令牌无效");
        }
    }
}
