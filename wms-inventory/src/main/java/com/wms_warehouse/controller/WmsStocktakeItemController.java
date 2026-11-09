package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsStocktakeItemPageDTO;
import com.wms_warehouse.dto.WmsStocktakeItemSaveDTO;
import com.wms_warehouse.dto.biz.CountRequest;
import com.wms_warehouse.service.WmsStocktakeBizService;
import com.wms_warehouse.service.WmsStocktakeItemService;
import com.wms_warehouse.entity.WmsStocktakeItem;
import com.wms_warehouse.vo.WmsStocktakeItemVO;
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
 * 盘点明细表 Controller
 */
@Tag(name = "盘点明细表管理", description = "盘点明细表管理")
@RestController
@RequestMapping("/wms_stocktake_item")
public class WmsStocktakeItemController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsStocktakeItemService wmsStocktakeItemService;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询盘点明细表")
@PostMapping("/page")
public Result<Page<WmsStocktakeItemVO>> page(@RequestBody WmsStocktakeItemPageDTO dto) {
    Page<WmsStocktakeItem> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsStocktakeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsStocktakeItem::getId);
    wmsStocktakeItemService.page(page, wrapper);
    Page<WmsStocktakeItemVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsStocktakeItemVO> records = page.getRecords().stream()
            .map(e -> {
                WmsStocktakeItemVO vo = new WmsStocktakeItemVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出盘点明细表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsStocktakeItemPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsStocktakeItem> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsStocktakeItem::getId);
    List<WmsStocktakeItem> rows = wmsStocktakeItemService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("盘点明细表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("盘点单");
        header.createCell(1).setCellValue("库存");
        header.createCell(2).setCellValue("SKU ID");
        header.createCell(3).setCellValue("库位");
        header.createCell(4).setCellValue("批次号");
        header.createCell(5).setCellValue("系统账面数量");
        header.createCell(6).setCellValue("实盘数量");
        header.createCell(7).setCellValue("差异数量");
        header.createCell(8).setCellValue("状态");
        header.createCell(9).setCellValue("盘点人");
        header.createCell(10).setCellValue("盘点时间");
        int rowIndex = 1;
        for (WmsStocktakeItem e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellNum(e.getStocktakeId()));
            row.createCell(1).setCellValue(cellNum(e.getInventoryId()));
            row.createCell(2).setCellValue(cellNum(e.getSkuId()));
            row.createCell(3).setCellValue(cellNum(e.getLocationId()));
            row.createCell(4).setCellValue(cellText(e.getBatchNo()));
            row.createCell(5).setCellValue(cellNum(e.getSystemQty()));
            row.createCell(6).setCellValue(cellNum(e.getCountedQty()));
            row.createCell(7).setCellValue(cellNum(e.getDiffQty()));
            row.createCell(8).setCellValue(cellText(e.getStatus()));
            row.createCell(9).setCellValue(cellNum(e.getCountedBy()));
            row.createCell(10).setCellValue(cellText(e.getCountedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("盘点明细表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询盘点明细表")
@GetMapping("/{id}")
public Result<WmsStocktakeItemVO> getById(@PathVariable("id") Long id) {
    WmsStocktakeItem entity = wmsStocktakeItemService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsStocktakeItemVO vo = new WmsStocktakeItemVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增盘点明细表")
@Log(title = "盘点明细表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsStocktakeItemSaveDTO dto) {
    WmsStocktakeItem entity = new WmsStocktakeItem();
    BeanUtils.copyProperties(dto, entity);
    wmsStocktakeItemService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改盘点明细表")
@Log(title = "盘点明细表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsStocktakeItemSaveDTO dto) {
    WmsStocktakeItem entity = new WmsStocktakeItem();
    BeanUtils.copyProperties(dto, entity);
    wmsStocktakeItemService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除盘点明细表")
@Log(title = "盘点明细表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsStocktakeItemService.removeById(id);
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
    private WmsStocktakeBizService wmsStocktakeBizService;

    @Operation(summary = "录实盘数量（差异由系统按快照计算）")
    @Log(title = "盘点明细", type = "UPDATE")
    @PostMapping("/{id}/count")
    public Result<Map<String, Object>> count(@PathVariable("id") Long id,
                                             @Valid @RequestBody CountRequest request) {
        return Result.ok(wmsStocktakeBizService.count(id, request));
    }

    /** V1.1 F-06：按盘点单 ID 一次取回全部明细，消除分页截断。 */
    @Operation(summary = "按盘点单 ID 查询全部明细")
    @GetMapping("/by-stocktake/{stocktakeId}")
    public Result<List<WmsStocktakeItem>> byStocktake(@PathVariable("stocktakeId") Long stocktakeId) {
        LambdaQueryWrapper<WmsStocktakeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WmsStocktakeItem::getStocktakeId, stocktakeId)
               .orderByAsc(WmsStocktakeItem::getId);
        return Result.ok(wmsStocktakeItemService.list(wrapper));
    }
}