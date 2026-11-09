package com.wms_warehouse.system;

import com.wms_warehouse.common.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典数据只读接口（V6.8 字典脚手架）：前端按 dict_code 取 value/label 列表。
 * 表结构与种子由 db/dict.sql 提供；端点在白名单里（/system/dict），免登录可读。
 * 必须返回 Result.ok(...) 统一包装：前端 request.js 按 res.code===200 判成功，
 * 裸 List 会让每个带字典的页面弹「请求失败」（坑库 spring-boot-3 字典只读接口条目）。
 */
@RestController
@RequestMapping("/system/dict")
public class SysDictController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/data/type/{code}")
    public Result<List<Map<String, String>>> byType(@PathVariable("code") String code) {
        List<Map<String, String>> out = new ArrayList<>();
        try {
            for (Map<String, Object> row : jdbcTemplate.queryForList(
                    "SELECT dict_value, dict_label FROM sys_dict_data "
                            + "WHERE dict_code = ? AND deleted = 0 ORDER BY sort, id", code)) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("value", String.valueOf(row.get("dict_value")));
                item.put("label", String.valueOf(row.get("dict_label")));
                out.add(item);
            }
        } catch (Exception e) {
            // 表没建/库抖动：返回空列表，前端下拉为空但页面不炸
            return Result.ok(List.of());
        }
        return Result.ok(out);
    }
}
