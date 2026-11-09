package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsOutboundOrderItemPageDTO;
import com.wms_warehouse.dto.WmsOutboundOrderItemSaveDTO;
import com.wms_warehouse.service.WmsOutboundOrderItemService;
import com.wms_warehouse.entity.WmsOutboundOrderItem;
import com.wms_warehouse.vo.WmsOutboundOrderItemVO;
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
 * 出库单明细表 Controller
 */
@Tag(name = "出库单明细表管理", description = "出库单明细表管理")
@RestController
@RequestMapping("/wms_outbound_order_item")
public class WmsOutboundOrderItemController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsOutboundOrderItemService wmsOutboundOrderItemService;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询出库单明细表")
@PostMapping("/page")
public Result<Page<WmsOutboundOrderItemVO>> page(@RequestBody WmsOutboundOrderItemPageDTO dto) {
    Page<WmsOutboundOrderItem> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsOutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsOutboundOrderItem::getId);
    wmsOutboundOrderItemService.page(page, wrapper);
    Page<WmsOutboundOrderItemVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsOutboundOrderItemVO> records = page.getRecords().stream()
            .map(e -> {
                WmsOutboundOrderItemVO vo = new WmsOutboundOrderItemVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出出库单明细表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsOutboundOrderItemPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsOutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsOutboundOrderItem::getId);
    List<WmsOutboundOrderItem> rows = wmsOutboundOrderItemService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("出库单明细表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("出库单");
        header.createCell(1).setCellValue("行号");
        header.createCell(2).setCellValue("SKU ID");
        header.createCell(3).setCellValue("批次号");
        header.createCell(4).setCellValue("订单数量");
        header.createCell(5).setCellValue("已分配数量");
        header.createCell(6).setCellValue("已拣数量");
        header.createCell(7).setCellValue("已发数量");
        header.createCell(8).setCellValue("状态");
        int rowIndex = 1;
        for (WmsOutboundOrderItem e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellNum(e.getOrderId()));
            row.createCell(1).setCellValue(cellNum(e.getLineNo()));
            row.createCell(2).setCellValue(cellNum(e.getSkuId()));
            row.createCell(3).setCellValue(cellText(e.getBatchNo()));
            row.createCell(4).setCellValue(cellNum(e.getOrderQty()));
            row.createCell(5).setCellValue(cellNum(e.getAllocatedQty()));
            row.createCell(6).setCellValue(cellNum(e.getPickedQty()));
            row.createCell(7).setCellValue(cellNum(e.getShippedQty()));
            row.createCell(8).setCellValue(cellText(e.getStatus()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("出库单明细表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询出库单明细表")
@GetMapping("/{id}")
public Result<WmsOutboundOrderItemVO> getById(@PathVariable("id") Long id) {
    WmsOutboundOrderItem entity = wmsOutboundOrderItemService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsOutboundOrderItemVO vo = new WmsOutboundOrderItemVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增出库单明细表")
@Log(title = "出库单明细表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsOutboundOrderItemSaveDTO dto) {
    WmsOutboundOrderItem entity = new WmsOutboundOrderItem();
    BeanUtils.copyProperties(dto, entity);
    wmsOutboundOrderItemService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改出库单明细表")
@Log(title = "出库单明细表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsOutboundOrderItemSaveDTO dto) {
    WmsOutboundOrderItem entity = new WmsOutboundOrderItem();
    BeanUtils.copyProperties(dto, entity);
    wmsOutboundOrderItemService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除出库单明细表")
@Log(title = "出库单明细表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsOutboundOrderItemService.removeById(id);
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

    /** V1.1 F-06：按出库单 ID 一次取回全部明细，消除分页截断。 */
    @Operation(summary = "按出库单 ID 查询全部明细")
    @GetMapping("/by-order/{orderId}")
    public Result<List<WmsOutboundOrderItem>> byOrder(@PathVariable("orderId") Long orderId) {
        LambdaQueryWrapper<WmsOutboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsOutboundOrderItem::getOrderId, orderId)
               .orderByAsc(WmsOutboundOrderItem::getLineNo)
               .orderByAsc(WmsOutboundOrderItem::getId);
        return Result.ok(wmsOutboundOrderItemService.list(wrapper));
    }
}
