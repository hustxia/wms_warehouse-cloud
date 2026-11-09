package com.wms_warehouse.common.idempotent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务端点幂等键 Mapper。
 *
 * <p>注意：本接口所在的包已加入各服务的 {@code @MapperScan} basePackages
 * （{@code com.wms_warehouse.common.idempotent}），新增服务时别忘了同步，否则启动即 no bean。
 */
@Mapper
public interface WmsRequestLogMapper extends BaseMapper<WmsRequestLog> {
}
