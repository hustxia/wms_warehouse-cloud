package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsInventoryTxnPageDTO;
import com.wms_warehouse.dto.WmsInventoryTxnSaveDTO;
import com.wms_warehouse.service.WmsInventoryTxnService;
import com.wms_warehouse.entity.WmsInventoryTxn;
import com.wms_warehouse.vo.WmsInventoryTxnVO;
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
 * 库存事务流水表 Controller
 */
@Tag(name = "库存事务流水表管理", description = "库存事务流水表管理")
@RestController
@RequestMapping("/wms_inventory_txn")
public class WmsInventoryTxnController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsInventoryTxnService wmsInventoryTxnService;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询库存事务流水表")
@PostMapping("/page")
public Result<Page<WmsInventoryTxnVO>> page(@RequestBody WmsInventoryTxnPageDTO dto) {
    Page<WmsInventoryTxn> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsInventoryTxn> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsInventoryTxn::getId);
    wmsInventoryTxnService.page(page, wrapper);
    Page<WmsInventoryTxnVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsInventoryTxnVO> records = page.getRecords().stream()
            .map(e -> {
                WmsInventoryTxnVO vo = new WmsInventoryTxnVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出库存事务流水表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsInventoryTxnPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsInventoryTxn> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsInventoryTxn::getId);
    List<WmsInventoryTxn> rows = wmsInventoryTxnService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("库存事务流水表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("事务流水号");
        header.createCell(1).setCellValue("类型");
        header.createCell(2).setCellValue("仓库");
        header.createCell(3).setCellValue("货主");
        header.createCell(4).setCellValue("SKU ID");
        header.createCell(5).setCellValue("库位");
        header.createCell(6).setCellValue("批次号");
        header.createCell(7).setCellValue("变动数量");
        header.createCell(8).setCellValue("变动前数量");
        header.createCell(9).setCellValue("变动后数量");
        header.createCell(10).setCellValue("关联单据号");
        header.createCell(11).setCellValue("操作人");
        header.createCell(12).setCellValue("备注");
        header.createCell(13).setCellValue("创建时间");
        int rowIndex = 1;
        for (WmsInventoryTxn e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellText(e.getTxnNo()));
            row.createCell(1).setCellValue(cellText(e.getTxnType()));
            row.createCell(2).setCellValue(cellNum(e.getWarehouseId()));
            row.createCell(3).setCellValue(cellNum(e.getOwnerId()));
            row.createCell(4).setCellValue(cellNum(e.getSkuId()));
            row.createCell(5).setCellValue(cellNum(e.getLocationId()));
            row.createCell(6).setCellValue(cellText(e.getBatchNo()));
            row.createCell(7).setCellValue(cellNum(e.getQtyChange()));
            row.createCell(8).setCellValue(cellNum(e.getQtyBefore()));
            row.createCell(9).setCellValue(cellNum(e.getQtyAfter()));
            row.createCell(10).setCellValue(cellText(e.getRefOrderNo()));
            row.createCell(11).setCellValue(cellNum(e.getOperatorId()));
            row.createCell(12).setCellValue(cellText(e.getRemark()));
            row.createCell(13).setCellValue(cellText(e.getCreatedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("库存事务流水表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询库存事务流水表")
@GetMapping("/{id}")
public Result<WmsInventoryTxnVO> getById(@PathVariable("id") Long id) {
    WmsInventoryTxn entity = wmsInventoryTxnService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsInventoryTxnVO vo = new WmsInventoryTxnVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增库存事务流水表")
@Log(title = "库存事务流水表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsInventoryTxnSaveDTO dto) {
    WmsInventoryTxn entity = new WmsInventoryTxn();
    BeanUtils.copyProperties(dto, entity);
    wmsInventoryTxnService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改库存事务流水表")
@Log(title = "库存事务流水表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsInventoryTxnSaveDTO dto) {
    WmsInventoryTxn entity = new WmsInventoryTxn();
    BeanUtils.copyProperties(dto, entity);
    wmsInventoryTxnService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除库存事务流水表")
@Log(title = "库存事务流水表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsInventoryTxnService.removeById(id);
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
