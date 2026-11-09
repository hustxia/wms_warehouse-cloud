package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsLocationPageDTO;
import com.wms_warehouse.dto.WmsLocationSaveDTO;
import com.wms_warehouse.service.WmsLocationService;
import com.wms_warehouse.entity.WmsLocation;
import com.wms_warehouse.vo.WmsLocationVO;
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
 * 库位表 Controller
 */
@Tag(name = "库位表管理", description = "库位表管理")
@RestController
@RequestMapping("/wms_location")
public class WmsLocationController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsLocationService wmsLocationService;
    @Autowired
    private com.wms_warehouse.common.util.DictTexts dictTexts;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询库位表")
@PostMapping("/page")
public Result<Page<WmsLocationVO>> page(@RequestBody WmsLocationPageDTO dto) {
    Page<WmsLocation> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsLocation> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsLocation::getId);
    wmsLocationService.page(page, wrapper);
    Page<WmsLocationVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsLocationVO> records = page.getRecords().stream()
            .map(e -> {
                WmsLocationVO vo = new WmsLocationVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出库位表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsLocationPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsLocation> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsLocation::getId);
    List<WmsLocation> rows = wmsLocationService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("库位表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("所属仓库");
        header.createCell(1).setCellValue("所属库区");
        header.createCell(2).setCellValue("库位编码");
        header.createCell(3).setCellValue("类型");
        header.createCell(4).setCellValue("排");
        header.createCell(5).setCellValue("列");
        header.createCell(6).setCellValue("层");
        header.createCell(7).setCellValue("最大容量");
        header.createCell(8).setCellValue("是否允许混放");
        header.createCell(9).setCellValue("状态");
        header.createCell(10).setCellValue("创建时间");
        int rowIndex = 1;
        for (WmsLocation e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellNum(e.getWarehouseId()));
            row.createCell(1).setCellValue(cellNum(e.getZoneId()));
            row.createCell(2).setCellValue(cellText(e.getLocationCode()));
            row.createCell(3).setCellValue(cellText(e.getLocationType()));
            row.createCell(4).setCellValue(cellNum(e.getRowNo()));
            row.createCell(5).setCellValue(cellNum(e.getColNo()));
            row.createCell(6).setCellValue(cellNum(e.getLayerNo()));
            row.createCell(7).setCellValue(cellNum(e.getMaxCapacity()));
            row.createCell(8).setCellValue(cellNum(e.getIsMixed()));
            row.createCell(9).setCellValue(cellNum(e.getStatus()));
            row.createCell(10).setCellValue(cellText(e.getCreatedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("库位表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询库位表")
@GetMapping("/{id}")
public Result<WmsLocationVO> getById(@PathVariable("id") Long id) {
    WmsLocation entity = wmsLocationService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsLocationVO vo = new WmsLocationVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增库位表")
@Log(title = "库位表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsLocationSaveDTO dto) {
    WmsLocation entity = new WmsLocation();
    BeanUtils.copyProperties(dto, entity);
    wmsLocationService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改库位表")
@Log(title = "库位表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsLocationSaveDTO dto) {
    WmsLocation entity = new WmsLocation();
    BeanUtils.copyProperties(dto, entity);
    wmsLocationService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除库位表")
@Log(title = "库位表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsLocationService.removeById(id);
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
