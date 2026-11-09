package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsOwnerPageDTO;
import com.wms_warehouse.dto.WmsOwnerSaveDTO;
import com.wms_warehouse.service.WmsOwnerService;
import com.wms_warehouse.entity.WmsOwner;
import com.wms_warehouse.vo.WmsOwnerVO;
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
 * 货主表 Controller
 */
@Tag(name = "货主表管理", description = "货主表管理")
@RestController
@RequestMapping("/wms_owner")
public class WmsOwnerController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsOwnerService wmsOwnerService;
    @Autowired
    private com.wms_warehouse.common.util.DictTexts dictTexts;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询货主表")
@PostMapping("/page")
public Result<Page<WmsOwnerVO>> page(@RequestBody WmsOwnerPageDTO dto) {
    Page<WmsOwner> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsOwner> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsOwner::getId);
    wmsOwnerService.page(page, wrapper);
    Page<WmsOwnerVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsOwnerVO> records = page.getRecords().stream()
            .map(e -> {
                WmsOwnerVO vo = new WmsOwnerVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出货主表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsOwnerPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsOwner> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsOwner::getId);
    List<WmsOwner> rows = wmsOwnerService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("货主表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("货主编码");
        header.createCell(1).setCellValue("货主名称");
        header.createCell(2).setCellValue("联系人");
        header.createCell(3).setCellValue("联系电话");
        header.createCell(4).setCellValue("状态");
        header.createCell(5).setCellValue("创建时间");
        int rowIndex = 1;
        for (WmsOwner e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellText(e.getOwnerCode()));
            row.createCell(1).setCellValue(cellText(e.getOwnerName()));
            row.createCell(2).setCellValue(cellText(e.getContact()));
            row.createCell(3).setCellValue(cellText(e.getPhone()));
            row.createCell(4).setCellValue(cellNum(e.getStatus()));
            row.createCell(5).setCellValue(cellText(e.getCreatedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("货主表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询货主表")
@GetMapping("/{id}")
public Result<WmsOwnerVO> getById(@PathVariable("id") Long id) {
    WmsOwner entity = wmsOwnerService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsOwnerVO vo = new WmsOwnerVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增货主表")
@Log(title = "货主表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsOwnerSaveDTO dto) {
    WmsOwner entity = new WmsOwner();
    BeanUtils.copyProperties(dto, entity);
    wmsOwnerService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改货主表")
@Log(title = "货主表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsOwnerSaveDTO dto) {
    WmsOwner entity = new WmsOwner();
    BeanUtils.copyProperties(dto, entity);
    wmsOwnerService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除货主表")
@Log(title = "货主表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsOwnerService.removeById(id);
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
