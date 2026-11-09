-- ============================================================
-- init_tables.sql：WMS 业务表结构（代码生成器 V7.2 交付物）
-- 由流水线 PPL20261104105301 的 db.sql 转换：DROP+CREATE -> IF NOT EXISTS（幂等）
-- 重复执行不会丢数据；若表结构已升级需变更，请走增量 DDL 而非重放本文件
-- ============================================================
CREATE DATABASE IF NOT EXISTS `wms_warehouse` DEFAULT CHARACTER SET utf8mb4;
-- ============================================================
-- 2.1 仓库表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_warehouse (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
    warehouse_code  VARCHAR(32)  NOT NULL                COMMENT '仓库编码',
    warehouse_name  VARCHAR(128) NOT NULL                COMMENT '仓库名称',
    address         VARCHAR(255)          DEFAULT NULL   COMMENT '仓库地址',
    contact_person  VARCHAR(64)           DEFAULT NULL   COMMENT '联系人',
    contact_phone   VARCHAR(32)           DEFAULT NULL   COMMENT '联系电话',
    status          TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (warehouse_code),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库主表';

-- ============================================================
-- 2.2 库区表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_zone (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库区ID',
    warehouse_id BIGINT       NOT NULL                COMMENT '所属仓库ID',
    zone_code    VARCHAR(32)  NOT NULL                COMMENT '库区编码',
    zone_name    VARCHAR(128) NOT NULL                COMMENT '库区名称',
    zone_type    VARCHAR(32)           DEFAULT NULL   COMMENT '类型:存储区/拣选区/收货区/发货区/退货区',
    status       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_zone_code (warehouse_id, zone_code),
    KEY idx_warehouse (warehouse_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库区表';

-- ============================================================
-- 2.3 库位表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_location (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '库位ID',
    warehouse_id  BIGINT       NOT NULL                COMMENT '所属仓库ID',
    zone_id       BIGINT       NOT NULL                COMMENT '所属库区ID',
    location_code VARCHAR(64)  NOT NULL                COMMENT '库位编码',
    location_type VARCHAR(32)           DEFAULT NULL   COMMENT '类型:横梁/流利/地堆/冷藏',
    row_no        INT                   DEFAULT NULL   COMMENT '排',
    col_no        INT                   DEFAULT NULL   COMMENT '列',
    layer_no      INT                   DEFAULT NULL   COMMENT '层',
    max_capacity  DECIMAL(12,3)         DEFAULT NULL   COMMENT '最大容量',
    is_mixed      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否允许混放:1是 0否',
    status        TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1可用 0锁定',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_location_code (warehouse_id, location_code),
    KEY idx_zone (zone_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

-- ============================================================
-- 2.4 商品SKU表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_sku (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    sku_code    VARCHAR(64)  NOT NULL                COMMENT 'SKU编码',
    sku_name    VARCHAR(255) NOT NULL                COMMENT '商品名称',
    barcode     VARCHAR(64)           DEFAULT NULL   COMMENT '条码',
    category_id BIGINT                DEFAULT NULL   COMMENT '分类ID',
    spec        VARCHAR(128)          DEFAULT NULL   COMMENT '规格',
    unit        VARCHAR(16)           DEFAULT NULL   COMMENT '计量单位',
    weight      DECIMAL(10,3)         DEFAULT NULL   COMMENT '重量(kg)',
    volume      DECIMAL(10,3)         DEFAULT NULL   COMMENT '体积(m³)',
    shelf_life  INT                   DEFAULT NULL   COMMENT '保质期(天)',
    is_batch    TINYINT      NOT NULL DEFAULT 0      COMMENT '是否批次管理:1是 0否',
    is_serial   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否序列号管理:1是 0否',
    status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sku_code (sku_code),
    KEY idx_barcode (barcode),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- ============================================================
-- 2.5 货主表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_owner (
    id         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '货主ID',
    owner_code VARCHAR(32)  NOT NULL                COMMENT '货主编码',
    owner_name VARCHAR(128) NOT NULL                COMMENT '货主名称',
    contact    VARCHAR(64)           DEFAULT NULL   COMMENT '联系人',
    phone      VARCHAR(32)           DEFAULT NULL   COMMENT '联系电话',
    status     TINYINT      NOT NULL DEFAULT 1      COMMENT '状态:1启用 0停用',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_owner_code (owner_code),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='货主表';

-- ============================================================
-- 3.1 库存表
-- 说明：唯一键为 (仓库+货主+SKU+库位+批次)，可用数量为生成列
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_inventory (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '库存ID',
    warehouse_id    BIGINT        NOT NULL                COMMENT '仓库ID',
    owner_id        BIGINT        NOT NULL                COMMENT '货主ID',
    sku_id          BIGINT        NOT NULL                COMMENT 'SKU ID',
    location_id     BIGINT        NOT NULL                COMMENT '库位ID',
    batch_no        VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    production_date DATE                   DEFAULT NULL   COMMENT '生产日期',
    expiry_date     DATE                   DEFAULT NULL   COMMENT '到期日期',
    qty_on_hand     DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '在库数量',
    qty_allocated   DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '已分配数量',
    qty_locked      DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '锁定数量',
    qty_available   DECIMAL(14,3) GENERATED ALWAYS AS (qty_on_hand - qty_allocated) STORED COMMENT '可用数量',
    version         INT           NOT NULL DEFAULT 0      COMMENT '乐观锁版本号',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_inv (warehouse_id, owner_id, sku_id, location_id, batch_no),
    KEY idx_sku (sku_id),
    KEY idx_location (location_id),
    KEY idx_expiry (expiry_date),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- ============================================================
-- 3.2 库存事务流水表（所有库存变动的留痕）
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_inventory_txn (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    txn_no       VARCHAR(64)   NOT NULL                COMMENT '事务流水号',
    txn_type     VARCHAR(32)   NOT NULL                COMMENT '类型:RECEIPT/PUTAWAY/PICK/SHIP/ADJUST/TRANSFER/MOVE',
    warehouse_id BIGINT        NOT NULL                COMMENT '仓库ID',
    owner_id     BIGINT        NOT NULL                COMMENT '货主ID',
    sku_id       BIGINT        NOT NULL                COMMENT 'SKU ID',
    location_id  BIGINT                 DEFAULT NULL   COMMENT '库位ID',
    batch_no     VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    qty_change   DECIMAL(14,3) NOT NULL                COMMENT '变动数量(正负)',
    qty_before   DECIMAL(14,3)          DEFAULT NULL   COMMENT '变动前数量',
    qty_after    DECIMAL(14,3)          DEFAULT NULL   COMMENT '变动后数量',
    ref_order_no VARCHAR(64)            DEFAULT NULL   COMMENT '关联单据号',
    operator_id  BIGINT                 DEFAULT NULL   COMMENT '操作人',
    remark       VARCHAR(255)           DEFAULT NULL   COMMENT '备注',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_txn_no (txn_no),
    KEY idx_sku_time (sku_id, created_at),
    KEY idx_ref_order (ref_order_no),
    KEY idx_type_time (txn_type, created_at),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存事务流水表';

-- ============================================================
-- 3.3 库存锁定表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_inventory_lock (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '锁定ID',
    lock_no      VARCHAR(64)   NOT NULL                COMMENT '锁定流水号',
    inventory_id BIGINT        NOT NULL                COMMENT '库存ID',
    sku_id       BIGINT        NOT NULL                COMMENT 'SKU ID',
    location_id  BIGINT        NOT NULL                COMMENT '库位ID',
    locked_qty   DECIMAL(14,3) NOT NULL                COMMENT '锁定数量',
    ref_order_no VARCHAR(64)            DEFAULT NULL   COMMENT '关联订单号',
    lock_type    VARCHAR(32)            DEFAULT NULL   COMMENT '锁定类型:PICK/TRANSFER/QC',
    status       TINYINT       NOT NULL DEFAULT 1      COMMENT '状态:1锁定中 0已释放',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    released_at  DATETIME               DEFAULT NULL   COMMENT '释放时间',
    PRIMARY KEY (id),
    KEY idx_inventory (inventory_id),
    KEY idx_ref_order (ref_order_no),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存锁定表';

-- ============================================================
-- 4.1 入库单主表
-- 状态流转：CREATED → IN_TRANSIT → RECEIVING → RECEIVED → PUTAWAY_DONE
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_inbound_order (
    id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '入库单ID',
    order_no      VARCHAR(64) NOT NULL                COMMENT '入库单号',
    order_type    VARCHAR(32) NOT NULL                COMMENT '类型:PURCHASE/RETURN/TRANSFER_IN',
    warehouse_id  BIGINT      NOT NULL                COMMENT '仓库ID',
    owner_id      BIGINT      NOT NULL                COMMENT '货主ID',
    supplier_id   BIGINT               DEFAULT NULL   COMMENT '供应商ID',
    ref_no        VARCHAR(64)          DEFAULT NULL   COMMENT '外部单号(ERP采购单号)',
    status        VARCHAR(32) NOT NULL DEFAULT 'CREATED'
                  COMMENT '状态:CREATED/IN_TRANSIT/RECEIVING/RECEIVED/PUTAWAY_DONE/CANCELLED',
    expected_date DATE                 DEFAULT NULL   COMMENT '预计到货日期',
    operator_id   BIGINT               DEFAULT NULL   COMMENT '操作人',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_status (status),
    KEY idx_ref_no (ref_no),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单主表';

-- ============================================================
-- 4.2 入库单明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_inbound_order_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    order_id     BIGINT        NOT NULL                COMMENT '入库单ID',
    line_no      INT           NOT NULL                COMMENT '行号',
    sku_id       BIGINT        NOT NULL                COMMENT 'SKU ID',
    batch_no     VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    expected_qty DECIMAL(14,3) NOT NULL                COMMENT '预期数量',
    received_qty DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '实收数量',
    putaway_qty  DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '已上架数量',
    location_id  BIGINT                 DEFAULT NULL   COMMENT '指定上架库位(可空)',
    status       VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING/PARTIAL/DONE',
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_sku (sku_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库单明细表';

-- ============================================================
-- 4.3 收货记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_receipt_record (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    order_id     BIGINT        NOT NULL                COMMENT '入库单ID',
    item_id      BIGINT        NOT NULL                COMMENT '入库单明细ID',
    sku_id       BIGINT        NOT NULL                COMMENT 'SKU ID',
    batch_no     VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    received_qty DECIMAL(14,3) NOT NULL                COMMENT '收货数量',
    location_id  BIGINT                 DEFAULT NULL   COMMENT '暂存库位',
    operator_id  BIGINT                 DEFAULT NULL   COMMENT '操作人',
    received_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收货时间',
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_item (item_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货记录表';

-- ============================================================
-- 4.4 上架任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_putaway_task (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    task_no       VARCHAR(64)   NOT NULL                COMMENT '上架任务号',
    order_id      BIGINT        NOT NULL                COMMENT '入库单ID',
    item_id       BIGINT        NOT NULL                COMMENT '入库单明细ID',
    sku_id        BIGINT        NOT NULL                COMMENT 'SKU ID',
    from_location BIGINT                 DEFAULT NULL   COMMENT '来源库位(暂存区)',
    to_location   BIGINT                 DEFAULT NULL   COMMENT '目标库位',
    batch_no      VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    task_qty      DECIMAL(14,3) NOT NULL                COMMENT '任务数量',
    done_qty      DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '已完成数量',
    status        VARCHAR(32)   NOT NULL DEFAULT 'PENDING'
                  COMMENT '状态:PENDING/ASSIGNED/DOING/DONE/CANCELLED',
    assignee_id   BIGINT                 DEFAULT NULL   COMMENT '作业员',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finished_at   DATETIME               DEFAULT NULL   COMMENT '完成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_status (status),
    KEY idx_order (order_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上架任务表';
-- ============================================================
-- 5.1 出库单主表
-- 状态流转：CREATED → ALLOCATED → PICKING → PICKED → SHIPPED
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_outbound_order (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '出库单ID',
    order_no     VARCHAR(64) NOT NULL                COMMENT '出库单号',
    order_type   VARCHAR(32) NOT NULL                COMMENT '类型:SALES/RETURN_TO_SUPPLIER/TRANSFER_OUT',
    warehouse_id BIGINT      NOT NULL                COMMENT '仓库ID',
    owner_id     BIGINT      NOT NULL                COMMENT '货主ID',
    customer_id  BIGINT               DEFAULT NULL   COMMENT '客户ID',
    ref_no       VARCHAR(64)          DEFAULT NULL   COMMENT '外部单号(ERP销售单号)',
    status       VARCHAR(32) NOT NULL DEFAULT 'CREATED'
                 COMMENT '状态:CREATED/ALLOCATED/PICKING/PICKED/SHIPPED/CANCELLED',
    priority     INT         NOT NULL DEFAULT 0      COMMENT '优先级',
    carrier      VARCHAR(64)          DEFAULT NULL   COMMENT '承运商',
    tracking_no  VARCHAR(64)          DEFAULT NULL   COMMENT '运单号',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_status (status),
    KEY idx_ref_no (ref_no),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单主表';

-- ============================================================
-- 5.2 出库单明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_outbound_order_item (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    order_id      BIGINT        NOT NULL                COMMENT '出库单ID',
    line_no       INT           NOT NULL                COMMENT '行号',
    sku_id        BIGINT        NOT NULL                COMMENT 'SKU ID',
    batch_no      VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    order_qty     DECIMAL(14,3) NOT NULL                COMMENT '订单数量',
    allocated_qty DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '已分配数量',
    picked_qty    DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '已拣数量',
    shipped_qty   DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '已发数量',
    status        VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '状态:PENDING/PARTIAL/DONE',
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_sku (sku_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库单明细表';

-- ============================================================
-- 5.3 库存分配明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_allocation (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '分配ID',
    order_id      BIGINT        NOT NULL                COMMENT '出库单ID',
    item_id       BIGINT        NOT NULL                COMMENT '出库单明细ID',
    inventory_id  BIGINT        NOT NULL                COMMENT '库存ID',
    sku_id        BIGINT        NOT NULL                COMMENT 'SKU ID',
    location_id   BIGINT        NOT NULL                COMMENT '库位ID',
    batch_no      VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    allocated_qty DECIMAL(14,3) NOT NULL                COMMENT '分配数量',
    status        TINYINT       NOT NULL DEFAULT 1      COMMENT '状态:1有效 0已释放',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_inventory (inventory_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存分配明细表';

-- ============================================================
-- 5.4 拣货任务表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_pick_task (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    task_no       VARCHAR(64)   NOT NULL                COMMENT '拣货任务号',
    order_id      BIGINT        NOT NULL                COMMENT '出库单ID',
    item_id       BIGINT        NOT NULL                COMMENT '出库单明细ID',
    allocation_id BIGINT                 DEFAULT NULL   COMMENT '关联分配明细ID',
    sku_id        BIGINT        NOT NULL                COMMENT 'SKU ID',
    location_id   BIGINT        NOT NULL                COMMENT '拣货库位ID',
    batch_no      VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    task_qty      DECIMAL(14,3) NOT NULL                COMMENT '任务数量',
    picked_qty    DECIMAL(14,3) NOT NULL DEFAULT 0      COMMENT '已拣数量',
    wave_no       VARCHAR(64)            DEFAULT NULL   COMMENT '波次号',
    status        VARCHAR(32)   NOT NULL DEFAULT 'PENDING'
                  COMMENT '状态:PENDING/ASSIGNED/DOING/DONE/EXCEPTION',
    assignee_id   BIGINT                 DEFAULT NULL   COMMENT '作业员',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finished_at   DATETIME               DEFAULT NULL   COMMENT '完成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_wave (wave_no),
    KEY idx_status (status),
    KEY idx_order (order_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拣货任务表';

-- ============================================================
-- 5.5 波次表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_wave (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '波次ID',
    wave_no      VARCHAR(64) NOT NULL                COMMENT '波次号',
    warehouse_id BIGINT      NOT NULL                COMMENT '仓库ID',
    wave_type    VARCHAR(32)          DEFAULT NULL   COMMENT '类型:按订单/按路线/按承运商',
    order_count  INT         NOT NULL DEFAULT 0      COMMENT '订单数量',
    status       VARCHAR(32) NOT NULL DEFAULT 'CREATED'
                 COMMENT '状态:CREATED/RELEASED/PICKING/DONE',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wave_no (wave_no),
    KEY idx_status (status),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='波次表';

-- ============================================================
-- 5.6 发货单表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_shipment (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '发货单ID',
    shipment_no   VARCHAR(64)  NOT NULL                COMMENT '发货单号',
    order_id      BIGINT       NOT NULL                COMMENT '出库单ID',
    carrier       VARCHAR(64)           DEFAULT NULL   COMMENT '承运商',
    tracking_no   VARCHAR(64)           DEFAULT NULL   COMMENT '运单号',
    package_count INT          NOT NULL DEFAULT 1      COMMENT '包裹数',
    total_weight  DECIMAL(10,3)         DEFAULT NULL   COMMENT '总重量(kg)',
    status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING'
                  COMMENT '状态:PENDING/SHIPPED/DELIVERED',
    shipped_at    DATETIME              DEFAULT NULL   COMMENT '发货时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shipment_no (shipment_no),
    KEY idx_order (order_id),
    KEY idx_tracking (tracking_no),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发货单表';

-- ============================================================
-- 6.1 盘点单表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_stocktake_order (
    id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '盘点单ID',
    stocktake_no   VARCHAR(64) NOT NULL                COMMENT '盘点单号',
    warehouse_id   BIGINT      NOT NULL                COMMENT '仓库ID',
    zone_id        BIGINT               DEFAULT NULL   COMMENT '盘点库区(可空=全仓)',
    stocktake_type VARCHAR(32)          DEFAULT NULL   COMMENT '类型:全盘/抽盘/循环盘点',
    status         VARCHAR(32) NOT NULL DEFAULT 'CREATED'
                   COMMENT '状态:CREATED/COUNTING/DIFF_PENDING/CONFIRMED/CANCELLED',
    created_by     BIGINT               DEFAULT NULL   COMMENT '创建人',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    confirmed_at   DATETIME             DEFAULT NULL   COMMENT '确认时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_stocktake_no (stocktake_no),
    KEY idx_status (status),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单表';

-- ============================================================
-- 6.2 盘点明细表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_stocktake_item (
    id           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    stocktake_id BIGINT        NOT NULL                COMMENT '盘点单ID',
    inventory_id BIGINT        NOT NULL                COMMENT '库存ID',
    sku_id       BIGINT        NOT NULL                COMMENT 'SKU ID',
    location_id  BIGINT        NOT NULL                COMMENT '库位ID',
    batch_no     VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    system_qty   DECIMAL(14,3) NOT NULL                COMMENT '系统账面数量',
    counted_qty  DECIMAL(14,3)          DEFAULT NULL   COMMENT '实盘数量',
    diff_qty     DECIMAL(14,3) GENERATED ALWAYS AS (IFNULL(counted_qty, 0) - system_qty) STORED COMMENT '差异数量',
    status       VARCHAR(32)   NOT NULL DEFAULT 'PENDING'
                 COMMENT '状态:PENDING/COUNTED/CONFIRMED',
    counted_by   BIGINT                 DEFAULT NULL   COMMENT '盘点人',
    counted_at   DATETIME               DEFAULT NULL   COMMENT '盘点时间',
    PRIMARY KEY (id),
    KEY idx_stocktake (stocktake_id),
    KEY idx_inventory (inventory_id),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点明细表';

-- ============================================================
-- 6.3 移库单表
-- ============================================================
CREATE TABLE IF NOT EXISTS wms_transfer_order (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '移库单ID',
    transfer_no   VARCHAR(64)   NOT NULL                COMMENT '移库单号',
    warehouse_id  BIGINT        NOT NULL                COMMENT '仓库ID',
    from_location BIGINT        NOT NULL                COMMENT '来源库位ID',
    to_location   BIGINT        NOT NULL                COMMENT '目标库位ID',
    sku_id        BIGINT        NOT NULL                COMMENT 'SKU ID',
    batch_no      VARCHAR(64)   NOT NULL DEFAULT ''     COMMENT '批次号',
    transfer_qty  DECIMAL(14,3) NOT NULL                COMMENT '移库数量',
    status        VARCHAR(32)   NOT NULL DEFAULT 'CREATED'
                  COMMENT '状态:CREATED/DOING/DONE/CANCELLED',
    operator_id   BIGINT                 DEFAULT NULL   COMMENT '操作人',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finished_at   DATETIME               DEFAULT NULL   COMMENT '完成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_no (transfer_no),
    KEY idx_status (status),
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` bigint NULL COMMENT '创建人',
  `update_by` bigint NULL COMMENT '更新人',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='移库单表';

SET FOREIGN_KEY_CHECKS = 1;
