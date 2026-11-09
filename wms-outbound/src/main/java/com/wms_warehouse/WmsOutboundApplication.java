package com.wms_warehouse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
@MapperScan(markerInterface = com.baomidou.mybatisplus.core.mapper.BaseMapper.class, basePackages = {"com.wms_warehouse.common.log", "com.wms_warehouse.common.idempotent", "com.wms_warehouse.mapper"})
public class WmsOutboundApplication {

    public static void main(String[] args) {
        SpringApplication.run(WmsOutboundApplication.class, args);
    }
}
