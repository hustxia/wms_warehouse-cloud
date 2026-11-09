package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsReceiptRecordPageDTO;
import com.wms_warehouse.dto.WmsReceiptRecordSaveDTO;
import com.wms_warehouse.service.WmsReceiptRecordService;
import com.wms_warehouse.entity.WmsReceiptRecord;
import com.wms_warehouse.vo.WmsReceiptRecordVO;
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
 * 收货记录表 Controller
 */
@Tag(name = "收货记录表管理", description = "收货记录表管理")
@RestController
@RequestMapping("/wms_receipt_record")
public class WmsReceiptRecordController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsReceiptRecordService wmsReceiptRecordService;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询收货记录表")
@PostMapping("/page")
public Result<Page<WmsReceiptRecordVO>> page(@RequestBody WmsReceiptRecordPageDTO dto) {
    Page<WmsReceiptRecord> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsReceiptRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsReceiptRecord::getId);
    wmsReceiptRecordService.page(page, wrapper);
    Page<WmsReceiptRecordVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsReceiptRecordVO> records = page.getRecords().stream()
            .map(e -> {
                WmsReceiptRecordVO vo = new WmsReceiptRecordVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出收货记录表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsReceiptRecordPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsReceiptRecord> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsReceiptRecord::getId);
    List<WmsReceiptRecord> rows = wmsReceiptRecordService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("收货记录表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("入库单");
        header.createCell(1).setCellValue("入库单明细");
        header.createCell(2).setCellValue("SKU ID");
        header.createCell(3).setCellValue("批次号");
        header.createCell(4).setCellValue("收货数量");
        header.createCell(5).setCellValue("暂存库位");
        header.createCell(6).setCellValue("操作人");
        header.createCell(7).setCellValue("收货时间");
        int rowIndex = 1;
        for (WmsReceiptRecord e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellNum(e.getOrderId()));
            row.createCell(1).setCellValue(cellNum(e.getItemId()));
            row.createCell(2).setCellValue(cellNum(e.getSkuId()));
            row.createCell(3).setCellValue(cellText(e.getBatchNo()));
            row.createCell(4).setCellValue(cellNum(e.getReceivedQty()));
            row.createCell(5).setCellValue(cellNum(e.getLocationId()));
            row.createCell(6).setCellValue(cellNum(e.getOperatorId()));
            row.createCell(7).setCellValue(cellText(e.getReceivedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("收货记录表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询收货记录表")
@GetMapping("/{id}")
public Result<WmsReceiptRecordVO> getById(@PathVariable("id") Long id) {
    WmsReceiptRecord entity = wmsReceiptRecordService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsReceiptRecordVO vo = new WmsReceiptRecordVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增收货记录表")
@Log(title = "收货记录表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsReceiptRecordSaveDTO dto) {
    WmsReceiptRecord entity = new WmsReceiptRecord();
    BeanUtils.copyProperties(dto, entity);
    wmsReceiptRecordService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改收货记录表")
@Log(title = "收货记录表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsReceiptRecordSaveDTO dto) {
    WmsReceiptRecord entity = new WmsReceiptRecord();
    BeanUtils.copyProperties(dto, entity);
    wmsReceiptRecordService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除收货记录表")
@Log(title = "收货记录表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsReceiptRecordService.removeById(id);
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
}
