-- V6.1 产物流程表（幂等：CREATE TABLE IF NOT EXISTS）
CREATE TABLE IF NOT EXISTS `sys_flow_def` (
  `id` bigint NOT NULL,
  `flow_code` varchar(64) NOT NULL COMMENT '流程编码',
  `flow_name` varchar(64) NOT NULL COMMENT '流程名称',
  `table_name` varchar(64) NOT NULL COMMENT '业务表名',
  `status_col` varchar(64) NOT NULL DEFAULT 'status' COMMENT '状态列',
  `nodes` varchar(1024) NOT NULL COMMENT '节点 JSON [{from,to,roleCode,rejectTo}]',
  `bpmn_xml` mediumtext NULL COMMENT 'V6.1 自动翻译的 BPMN XML',
  `bpmn_hash` varchar(64) DEFAULT NULL COMMENT '已部署的 bpmn_xml sha256（幂等部署标记）',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_flow_code` (`flow_code`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流程定义';

CREATE TABLE IF NOT EXISTS `sys_flow_task` (
  `id` bigint NOT NULL,
  `flow_code` varchar(64) NOT NULL,
  `biz_table` varchar(64) NOT NULL,
  `biz_id` bigint NOT NULL,
  `from_status` varchar(20) DEFAULT NULL,
  `to_status` varchar(20) DEFAULT NULL,
  `action` varchar(20) NOT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `oper_user_id` bigint DEFAULT NULL,
  `oper_name` varchar(64) DEFAULT NULL,
  `oper_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_flow_biz` (`flow_code`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流水';
