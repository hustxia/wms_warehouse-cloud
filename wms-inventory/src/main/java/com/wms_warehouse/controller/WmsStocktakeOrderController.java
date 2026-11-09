package com.wms_warehouse.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wms_warehouse.dto.WmsStocktakeOrderPageDTO;
import com.wms_warehouse.dto.WmsStocktakeOrderSaveDTO;
import com.wms_warehouse.dto.biz.RequestIdRequest;
import com.wms_warehouse.dto.biz.StocktakeCreateRequest;
import com.wms_warehouse.service.WmsStocktakeBizService;
import com.wms_warehouse.service.WmsStocktakeOrderService;
import com.wms_warehouse.entity.WmsStocktakeOrder;
import com.wms_warehouse.vo.WmsStocktakeOrderVO;
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
 * 盘点单表 Controller
 */
@Tag(name = "盘点单表管理", description = "盘点单表管理")
@RestController
@RequestMapping("/wms_stocktake_order")
public class WmsStocktakeOrderController {

    // Service 注入无条件保留（理由见 import 区注释）
    @Autowired
    private WmsStocktakeOrderService wmsStocktakeOrderService;
    // <gen:begin key="controller-body">

@Operation(summary = "分页查询盘点单表")
@PostMapping("/page")
public Result<Page<WmsStocktakeOrderVO>> page(@RequestBody WmsStocktakeOrderPageDTO dto) {
    Page<WmsStocktakeOrder> page = new Page<>(dto.getPageNum(), dto.getPageSize());
    LambdaQueryWrapper<WmsStocktakeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(WmsStocktakeOrder::getId);
    wmsStocktakeOrderService.page(page, wrapper);
    Page<WmsStocktakeOrderVO> voPage = new Page<>();
    BeanUtils.copyProperties(page, voPage);
    List<WmsStocktakeOrderVO> records = page.getRecords().stream()
            .map(e -> {
                WmsStocktakeOrderVO vo = new WmsStocktakeOrderVO();
                BeanUtils.copyProperties(e, vo);
                return vo;
            })
            .toList();
    voPage.setRecords(records);
    return Result.ok(voPage);
}

@Operation(summary = "导出盘点单表 Excel（按查询条件过滤；不传条件导出全部）")
@PostMapping("/export")
public ResponseEntity<byte[]> export(@RequestBody(required = false) WmsStocktakeOrderPageDTO query) throws IOException {
    // 复用列表页的筛选口径：导出的行必须和用户在页面上看到的一致，
    // 否则「筛出 20 条、导出 2000 条」会被当成 bug。
    LambdaQueryWrapper<WmsStocktakeOrder> wrapper = new LambdaQueryWrapper<>();
    if (query != null) {
    }
    wrapper.orderByDesc(WmsStocktakeOrder::getId);
    List<WmsStocktakeOrder> rows = wmsStocktakeOrderService.list(wrapper);
    try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
        Sheet sheet = wb.createSheet("盘点单表");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("盘点单号");
        header.createCell(1).setCellValue("仓库");
        header.createCell(2).setCellValue("盘点库区");
        header.createCell(3).setCellValue("类型");
        header.createCell(4).setCellValue("状态");
        header.createCell(5).setCellValue("创建人");
        header.createCell(6).setCellValue("创建时间");
        header.createCell(7).setCellValue("确认时间");
        int rowIndex = 1;
        for (WmsStocktakeOrder e : rows) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(cellText(e.getStocktakeNo()));
            row.createCell(1).setCellValue(cellNum(e.getWarehouseId()));
            row.createCell(2).setCellValue(cellNum(e.getZoneId()));
            row.createCell(3).setCellValue(cellText(e.getStocktakeType()));
            row.createCell(4).setCellValue(cellText(e.getStatus()));
            row.createCell(5).setCellValue(cellNum(e.getCreatedBy()));
            row.createCell(6).setCellValue(cellText(e.getCreatedAt()));
            row.createCell(7).setCellValue(cellText(e.getConfirmedAt()));
        }
        wb.write(bos);
        String fileName = URLEncoder.encode("盘点单表.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bos.toByteArray());
    }
}

@Operation(summary = "根据ID查询盘点单表")
@GetMapping("/{id}")
public Result<WmsStocktakeOrderVO> getById(@PathVariable("id") Long id) {
    WmsStocktakeOrder entity = wmsStocktakeOrderService.getById(id);
    if (entity == null) {
        return Result.ok(null);
    }
    WmsStocktakeOrderVO vo = new WmsStocktakeOrderVO();
    BeanUtils.copyProperties(entity, vo);
    return Result.ok(vo);
}

@Operation(summary = "新增盘点单表")
@Log(title = "盘点单表", type = "INSERT")
@PostMapping
public Result<Void> add(@Valid @RequestBody WmsStocktakeOrderSaveDTO dto) {
    WmsStocktakeOrder entity = new WmsStocktakeOrder();
    BeanUtils.copyProperties(dto, entity);
    wmsStocktakeOrderService.save(entity);
    return Result.ok();
}

@Operation(summary = "修改盘点单表")
@Log(title = "盘点单表", type = "UPDATE")
@PutMapping
public Result<Void> update(@Valid @RequestBody WmsStocktakeOrderSaveDTO dto) {
    WmsStocktakeOrder entity = new WmsStocktakeOrder();
    BeanUtils.copyProperties(dto, entity);
    wmsStocktakeOrderService.updateById(entity);
    return Result.ok();
}

@Operation(summary = "删除盘点单表")
@Log(title = "盘点单表", type = "DELETE")
@DeleteMapping("/{id}")
public Result<Void> delete(@PathVariable("id") Long id) {
    wmsStocktakeOrderService.removeById(id);
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

    /**
     * 创建盘点单。路径用 /create 而不是裸 POST：
     * 生成器已经在同一个类里占用了 {@code POST /wms_stocktake_order}（主数据新增），
     * 两个 handler 映射同一路径会让 Spring 启动即报 ambiguous mapping。
     */
    @Operation(summary = "创建盘点单（按范围快照库存生成明细）")
    @Log(title = "盘点单", type = "INSERT")
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@Valid @RequestBody StocktakeCreateRequest request) {
        return Result.ok(wmsStocktakeBizService.create(request));
    }

    @Operation(summary = "冻结（锁住可用量，冻结期间不可被分配）")
    @Log(title = "盘点单", type = "UPDATE")
    @PostMapping("/{id}/freeze")
    public Result<Map<String, Object>> freeze(@PathVariable("id") Long id,
                                              @Valid @RequestBody RequestIdRequest request) {
        return Result.ok(wmsStocktakeBizService.freeze(id, request.getRequestId()));
    }

    @Operation(summary = "确认调整（按差异调整在库量并释放冻结）")
    @Log(title = "盘点单", type = "UPDATE")
    @PostMapping("/{id}/confirm")
    public Result<Map<String, Object>> confirm(@PathVariable("id") Long id,
                                               @Valid @RequestBody RequestIdRequest request) {
        return Result.ok(wmsStocktakeBizService.confirm(id, request.getRequestId()));
    }
}