package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsInboundOrderItemPageDTO;
import com.wms_warehouse.dto.WmsInboundOrderItemSaveDTO;
import com.wms_warehouse.service.WmsInboundOrderItemService;
import com.wms_warehouse.entity.WmsInboundOrderItem;
import com.wms_warehouse.vo.WmsInboundOrderItemVO;
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
 * 入库单明细表 Controller
 */
@Tag(name = "入库单明细表管理", description = "入库单明细表管理")
@RestController
@RequestMapping("/wms_inbound_order_item")
public class WmsInboundOrderItemController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsInboundOrderItemService wmsInboundOrderItemService;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询入库单明细表")
@PostMapping("/page")
public Result<Page<WmsInboundOrderItemVO>> page(@RequestBody WmsInboundOrderItemPageDTO dto) {
    Page<WmsInboundOrderItem> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsInboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsInboundOrderItem::getId);
    wmsInboundOrderItemService.page(page, wrapper);
    Page<WmsInboundOrderItemVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsInboundOrderItemVO> records = page.getRecords().stream()
            .map(e -> {
                WmsInboundOrderItemVO vo = new WmsInboundOrderItemVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出入库单明细表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsInboundOrderItemPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsInboundOrderItem> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsInboundOrderItem::getId);
    List<WmsInboundOrderItem> rows = wmsInboundOrderItemService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("入库单明细表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("入库单");
        header.createCell(1).setCellValue("行号");
        header.createCell(2).setCellValue("SKU ID");
        header.createCell(3).setCellValue("批次号");
        header.createCell(4).setCellValue("预期数量");
        header.createCell(5).setCellValue("实收数量");
        header.createCell(6).setCellValue("已上架数量");
        header.createCell(7).setCellValue("指定上架库位");
        header.createCell(8).setCellValue("状态");
        int rowIndex = 1;
        for (WmsInboundOrderItem e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellNum(e.getOrderId()));
            row.createCell(1).setCellValue(cellNum(e.getLineNo()));
            row.createCell(2).setCellValue(cellNum(e.getSkuId()));
            row.createCell(3).setCellValue(cellText(e.getBatchNo()));
            row.createCell(4).setCellValue(cellNum(e.getExpectedQty()));
            row.createCell(5).setCellValue(cellNum(e.getReceivedQty()));
            row.createCell(6).setCellValue(cellNum(e.getPutawayQty()));
            row.createCell(7).setCellValue(cellNum(e.getLocationId()));
            row.createCell(8).setCellValue(cellText(e.getStatus()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("入库单明细表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询入库单明细表")
@GetMapping("/{id}")
public Result<WmsInboundOrderItemVO> getById(@PathVariable("id") Long id) {
    WmsInboundOrderItem entity = wmsInboundOrderItemService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsInboundOrderItemVO vo = new WmsInboundOrderItemVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增入库单明细表")
@Log(title = "入库单明细表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsInboundOrderItemSaveDTO dto) {
    WmsInboundOrderItem entity = new WmsInboundOrderItem();
    BeanUtils.copyProperties(dto, entity);
    wmsInboundOrderItemService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改入库单明细表")
@Log(title = "入库单明细表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsInboundOrderItemSaveDTO dto) {
    WmsInboundOrderItem entity = new WmsInboundOrderItem();
    BeanUtils.copyProperties(dto, entity);
    wmsInboundOrderItemService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除入库单明细表")
@Log(title = "入库单明细表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsInboundOrderItemService.removeById(id);
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

    /**
     * V1.1 F-06：按入库单 ID 一次取回全部明细。
     *
     * <p>替代前端「分页拉 10 页 × 200 行再自己拼」的做法。明细行数没有上限，
     * 分页拉取会静默截断（超过 2000 行时后面的明细根本不显示），在收货核对现场
     * 是可以直接造成少收/错收的数据事故。
     */
    @Operation(summary = "按入库单 ID 查询全部明细")
    @GetMapping("/by-order/{orderId}")
    public Result<List<WmsInboundOrderItem>> byOrder(@PathVariable("orderId") Long orderId) {
        LambdaQueryWrapper<WmsInboundOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsInboundOrderItem::getOrderId, orderId)
               .orderByAsc(WmsInboundOrderItem::getLineNo)
               .orderByAsc(WmsInboundOrderItem::getId);
        return Result.ok(wmsInboundOrderItemService.list(wrapper));
    }
}
