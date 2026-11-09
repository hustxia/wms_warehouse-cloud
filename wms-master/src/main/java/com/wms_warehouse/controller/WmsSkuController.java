package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsSkuPageDTO;
import com.wms_warehouse.dto.WmsSkuSaveDTO;
import com.wms_warehouse.service.WmsSkuService;
import com.wms_warehouse.entity.WmsSku;
import com.wms_warehouse.vo.WmsSkuVO;
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
 * 商品SKU表 Controller
 */
@Tag(name = "商品SKU表管理", description = "商品SKU表管理")
@RestController
@RequestMapping("/wms_sku")
public class WmsSkuController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsSkuService wmsSkuService;
    @Autowired
    private com.wms_warehouse.common.util.DictTexts dictTexts;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询商品SKU表")
@PostMapping("/page")
public Result<Page<WmsSkuVO>> page(@RequestBody WmsSkuPageDTO dto) {
    Page<WmsSku> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsSku> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsSku::getId);
    wmsSkuService.page(page, wrapper);
    Page<WmsSkuVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsSkuVO> records = page.getRecords().stream()
            .map(e -> {
                WmsSkuVO vo = new WmsSkuVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出商品SKU表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsSkuPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsSku> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsSku::getId);
    List<WmsSku> rows = wmsSkuService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("商品SKU表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("SKU编码");
        header.createCell(1).setCellValue("商品名称");
        header.createCell(2).setCellValue("条码");
        header.createCell(3).setCellValue("分类");
        header.createCell(4).setCellValue("规格");
        header.createCell(5).setCellValue("计量单位");
        header.createCell(6).setCellValue("重量");
        header.createCell(7).setCellValue("体积");
        header.createCell(8).setCellValue("保质期");
        header.createCell(9).setCellValue("是否批次管理");
        header.createCell(10).setCellValue("是否序列号管理");
        header.createCell(11).setCellValue("状态");
        header.createCell(12).setCellValue("创建时间");
        int rowIndex = 1;
        for (WmsSku e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellText(e.getSkuCode()));
            row.createCell(1).setCellValue(cellText(e.getSkuName()));
            row.createCell(2).setCellValue(cellText(e.getBarcode()));
            row.createCell(3).setCellValue(cellNum(e.getCategoryId()));
            row.createCell(4).setCellValue(cellText(e.getSpec()));
            row.createCell(5).setCellValue(cellText(e.getUnit()));
            row.createCell(6).setCellValue(cellNum(e.getWeight()));
            row.createCell(7).setCellValue(cellNum(e.getVolume()));
            row.createCell(8).setCellValue(cellNum(e.getShelfLife()));
            row.createCell(9).setCellValue(cellNum(e.getIsBatch()));
            row.createCell(10).setCellValue(cellNum(e.getIsSerial()));
            row.createCell(11).setCellValue(cellNum(e.getStatus()));
            row.createCell(12).setCellValue(cellText(e.getCreatedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("商品SKU表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询商品SKU表")
@GetMapping("/{id}")
public Result<WmsSkuVO> getById(@PathVariable("id") Long id) {
    WmsSku entity = wmsSkuService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsSkuVO vo = new WmsSkuVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增商品SKU表")
@Log(title = "商品SKU表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsSkuSaveDTO dto) {
    WmsSku entity = new WmsSku();
    BeanUtils.copyProperties(dto, entity);
    wmsSkuService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改商品SKU表")
@Log(title = "商品SKU表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsSkuSaveDTO dto) {
    WmsSku entity = new WmsSku();
    BeanUtils.copyProperties(dto, entity);
    wmsSkuService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除商品SKU表")
@Log(title = "商品SKU表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsSkuService.removeById(id);
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
