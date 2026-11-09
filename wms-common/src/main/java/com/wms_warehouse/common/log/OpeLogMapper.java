package com.wms_warehouse.common.log;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface OpeLogMapper extends BaseMapper<OpeLog> {
}
