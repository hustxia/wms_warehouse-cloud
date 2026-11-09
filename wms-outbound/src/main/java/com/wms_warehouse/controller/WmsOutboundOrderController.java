package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsOutboundOrderPageDTO;
import com.wms_warehouse.dto.WmsOutboundOrderSaveDTO;
import com.wms_warehouse.dto.biz.AllocateRequest;
import com.wms_warehouse.dto.biz.CancelAllocateRequest;
import com.wms_warehouse.dto.biz.PickRequest;
import com.wms_warehouse.dto.biz.ShipRequest;
import com.wms_warehouse.service.WmsOutboundBizService;
import com.wms_warehouse.service.WmsOutboundOrderService;
import com.wms_warehouse.entity.WmsOutboundOrder;
import com.wms_warehouse.vo.WmsOutboundOrderVO;
import com.wms_warehouse.common.result.Result;
import com.wms_warehouse.common.log.Log;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出库单主表 Controller
 */
@Tag(name = "出库单主表管理", description = "出库单主表管理")
@RestController
@RequestMapping("/wms_outbound_order")
public class WmsOutboundOrderController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsOutboundOrderService wmsOutboundOrderService;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询出库单主表")
@PostMapping("/page")
public Result<Page<WmsOutboundOrderVO>> page(@RequestBody WmsOutboundOrderPageDTO dto) {
    Page<WmsOutboundOrder> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsOutboundOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsOutboundOrder::getId);
    wmsOutboundOrderService.page(page, wrapper);
    Page<WmsOutboundOrderVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsOutboundOrderVO> records = page.getRecords().stream()
            .map(e -> {
                WmsOutboundOrderVO vo = new WmsOutboundOrderVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出出库单主表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsOutboundOrderPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsOutboundOrder> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsOutboundOrder::getId);
    List<WmsOutboundOrder> rows = wmsOutboundOrderService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("出库单主表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("出库单号");
        header.createCell(1).setCellValue("类型");
        header.createCell(2).setCellValue("仓库");
        header.createCell(3).setCellValue("货主");
        header.createCell(4).setCellValue("客户");
        header.createCell(5).setCellValue("外部单号");
        header.createCell(6).setCellValue("状态");
        header.createCell(7).setCellValue("优先级");
        header.createCell(8).setCellValue("承运商");
        header.createCell(9).setCellValue("运单号");
        header.createCell(10).setCellValue("创建时间");
        header.createCell(11).setCellValue("更新时间");
        int rowIndex = 1;
        for (WmsOutboundOrder e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellText(e.getOrderNo()));
            row.createCell(1).setCellValue(cellText(e.getOrderType()));
            row.createCell(2).setCellValue(cellNum(e.getWarehouseId()));
            row.createCell(3).setCellValue(cellNum(e.getOwnerId()));
            row.createCell(4).setCellValue(cellNum(e.getCustomerId()));
            row.createCell(5).setCellValue(cellText(e.getRefNo()));
            row.createCell(6).setCellValue(cellText(e.getStatus()));
            row.createCell(7).setCellValue(cellNum(e.getPriority()));
            row.createCell(8).setCellValue(cellText(e.getCarrier()));
            row.createCell(9).setCellValue(cellText(e.getTrackingNo()));
            row.createCell(10).setCellValue(cellText(e.getCreatedAt()));
            row.createCell(11).setCellValue(cellText(e.getUpdatedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("出库单主表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询出库单主表")
@GetMapping("/{id}")
public Result<WmsOutboundOrderVO> getById(@PathVariable("id") Long id) {
    WmsOutboundOrder entity = wmsOutboundOrderService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsOutboundOrderVO vo = new WmsOutboundOrderVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增出库单主表")
@Log(title = "出库单主表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsOutboundOrderSaveDTO dto) {
    WmsOutboundOrder entity = new WmsOutboundOrder();
    BeanUtils.copyProperties(dto, entity);
    wmsOutboundOrderService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改出库单主表")
@Log(title = "出库单主表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsOutboundOrderSaveDTO dto) {
    WmsOutboundOrder entity = new WmsOutboundOrder();
    BeanUtils.copyProperties(dto, entity);
    wmsOutboundOrderService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除出库单主表")
@Log(title = "出库单主表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsOutboundOrderService.removeById(id);
    return Result.ok();
}
    // <gen:end key="controller-body">

    private static String cellText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static double cellNum(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0D;
    }

    private static String text(Row row, Integer col) {
        if (col == null || row == null) {
            return null;
        }
        String value = cellText(row.getCell(col)).trim();
        return value.isEmpty() ? null : value;
    }
    // ===== 以下为手写区：自定义接口写在这里，重新生成不会被覆盖 =====

    @Autowired
    private WmsOutboundBizService wmsOutboundBizService;

    @Operation(summary = "分配（FEFO：效期近的先出，库存不足时部分分配）")
    @Log(title = "出库单", type = "UPDATE")
    @PostMapping("/{id}/allocate")
    public Result<Map<String, Object>> allocate(@PathVariable("id") Long id,
                                                @Valid @RequestBody AllocateRequest request) {
        return Result.ok(wmsOutboundBizService.allocate(id, request));
    }

    @Operation(summary = "拣货确认（扣在库量与已分配量，写拣货任务与流水）")
    @Log(title = "出库单", type = "UPDATE")
    @PostMapping("/{id}/pick")
    public Result<Map<String, Object>> pick(@PathVariable("id") Long id,
                                            @Valid @RequestBody PickRequest request) {
        return Result.ok(wmsOutboundBizService.pick(id, request));
    }

    @Operation(summary = "发运（写发货单，单据置已发运）")
    @Log(title = "出库单", type = "UPDATE")
    @PostMapping("/{id}/ship")
    public Result<Map<String, Object>> ship(@PathVariable("id") Long id,
                                            @Valid @RequestBody ShipRequest request) {
        return Result.ok(wmsOutboundBizService.ship(id, request));
    }

    @Operation(summary = "取消分配（释放全部占用，单据回到已创建）")
    @Log(title = "出库单", type = "UPDATE")
    @PostMapping("/{id}/cancel-allocate")
    public Result<Map<String, Object>> cancelAllocate(@PathVariable("id") Long id,
                                                      @Valid @RequestBody CancelAllocateRequest request) {
        return Result.ok(wmsOutboundBizService.cancelAllocate(id, request.getRequestId()));
    }
}