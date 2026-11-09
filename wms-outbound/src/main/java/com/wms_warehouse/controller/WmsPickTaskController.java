package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsPickTaskPageDTO;
import com.wms_warehouse.dto.WmsPickTaskSaveDTO;
import com.wms_warehouse.service.WmsPickTaskService;
import com.wms_warehouse.entity.WmsPickTask;
import com.wms_warehouse.vo.WmsPickTaskVO;
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
 * 拣货任务表 Controller
 */
@Tag(name = "拣货任务表管理", description = "拣货任务表管理")
@RestController
@RequestMapping("/wms_pick_task")
public class WmsPickTaskController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsPickTaskService wmsPickTaskService;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询拣货任务表")
@PostMapping("/page")
public Result<Page<WmsPickTaskVO>> page(@RequestBody WmsPickTaskPageDTO dto) {
    Page<WmsPickTask> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsPickTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsPickTask::getId);
    wmsPickTaskService.page(page, wrapper);
    Page<WmsPickTaskVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsPickTaskVO> records = page.getRecords().stream()
            .map(e -> {
                WmsPickTaskVO vo = new WmsPickTaskVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出拣货任务表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsPickTaskPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsPickTask> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsPickTask::getId);
    List<WmsPickTask> rows = wmsPickTaskService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("拣货任务表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("拣货任务号");
        header.createCell(1).setCellValue("出库单");
        header.createCell(2).setCellValue("出库单明细");
        header.createCell(3).setCellValue("关联分配明细");
        header.createCell(4).setCellValue("SKU ID");
        header.createCell(5).setCellValue("拣货库位");
        header.createCell(6).setCellValue("批次号");
        header.createCell(7).setCellValue("任务数量");
        header.createCell(8).setCellValue("已拣数量");
        header.createCell(9).setCellValue("波次号");
        header.createCell(10).setCellValue("状态");
        header.createCell(11).setCellValue("作业员");
        header.createCell(12).setCellValue("创建时间");
        header.createCell(13).setCellValue("完成时间");
        int rowIndex = 1;
        for (WmsPickTask e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellText(e.getTaskNo()));
            row.createCell(1).setCellValue(cellNum(e.getOrderId()));
            row.createCell(2).setCellValue(cellNum(e.getItemId()));
            row.createCell(3).setCellValue(cellNum(e.getAllocationId()));
            row.createCell(4).setCellValue(cellNum(e.getSkuId()));
            row.createCell(5).setCellValue(cellNum(e.getLocationId()));
            row.createCell(6).setCellValue(cellText(e.getBatchNo()));
            row.createCell(7).setCellValue(cellNum(e.getTaskQty()));
            row.createCell(8).setCellValue(cellNum(e.getPickedQty()));
            row.createCell(9).setCellValue(cellText(e.getWaveNo()));
            row.createCell(10).setCellValue(cellText(e.getStatus()));
            row.createCell(11).setCellValue(cellNum(e.getAssigneeId()));
            row.createCell(12).setCellValue(cellText(e.getCreatedAt()));
            row.createCell(13).setCellValue(cellText(e.getFinishedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("拣货任务表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询拣货任务表")
@GetMapping("/{id}")
public Result<WmsPickTaskVO> getById(@PathVariable("id") Long id) {
    WmsPickTask entity = wmsPickTaskService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsPickTaskVO vo = new WmsPickTaskVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增拣货任务表")
@Log(title = "拣货任务表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsPickTaskSaveDTO dto) {
    WmsPickTask entity = new WmsPickTask();
    BeanUtils.copyProperties(dto, entity);
    wmsPickTaskService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改拣货任务表")
@Log(title = "拣货任务表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsPickTaskSaveDTO dto) {
    WmsPickTask entity = new WmsPickTask();
    BeanUtils.copyProperties(dto, entity);
    wmsPickTaskService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除拣货任务表")
@Log(title = "拣货任务表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsPickTaskService.removeById(id);
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
