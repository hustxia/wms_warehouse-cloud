-- 骨架自带登录基座与权限体系：admin / admin123
-- 由建系统流水线在建库后自动执行；手工部署时也可以手动跑本文件
-- V6.4：密码 BCrypt（password_algo 标记算法，旧 SHA-256 存量用户登录时无感迁移）
CREATE TABLE IF NOT EXISTS sys_user (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  username varchar(64) NOT NULL COMMENT '登录名',
  password varchar(72) NOT NULL COMMENT '密码（BCrypt 60位，兼容旧 SHA-256 64位）',
  password_algo varchar(16) NOT NULL DEFAULT 'bcrypt' COMMENT '密码算法：bcrypt/sha256',
  fail_count int NOT NULL DEFAULT 0 COMMENT '连续失败次数（V6.4 登录锁定）',
  lock_until datetime NULL COMMENT '锁定截止时间',
  nickname varchar(64) NULL COMMENT '昵称',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  create_by bigint NULL COMMENT '创建人',
  update_by bigint NULL COMMENT '更新人',
  deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS sys_session (
  token varchar(64) NOT NULL COMMENT '登录令牌',
  username varchar(64) NOT NULL COMMENT '登录名',
  login_ip varchar(64) NULL COMMENT '登录IP',
  login_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  expire_time datetime NOT NULL COMMENT '过期时间（滑动续期）',
  deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (token),
  KEY idx_session_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录会话（V6.4 持久化，重启不掉线）';

CREATE TABLE IF NOT EXISTS sys_login_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  username varchar(64) NULL COMMENT '尝试登录的用户名',
  ip varchar(64) NULL COMMENT '来源IP',
  region varchar(128) NULL COMMENT 'IP归属地',
  success tinyint NOT NULL DEFAULT 0 COMMENT '是否成功：1是0否',
  fail_reason varchar(255) NULL COMMENT '失败原因',
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (id),
  KEY idx_login_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志（V6.4）';

CREATE TABLE IF NOT EXISTS sys_oper_log (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  title varchar(64) NULL COMMENT '模块标题',
  oper_type varchar(32) NULL COMMENT '操作类型',
  method varchar(128) NULL COMMENT '方法名',
  request_uri varchar(128) NULL COMMENT '请求URI',
  request_method varchar(16) NULL COMMENT '请求方式',
  oper_param text NULL COMMENT '请求参数',
  json_result text NULL COMMENT '返回结果',
  status tinyint NULL COMMENT '1成功 0失败',
  error_msg varchar(512) NULL COMMENT '错误消息',
  oper_user_id bigint NULL COMMENT '操作人ID',
  oper_name varchar(64) NULL COMMENT '操作人',
  oper_ip varchar(64) NULL COMMENT '来源IP',
  province varchar(32) NULL COMMENT '省份',
  city varchar(32) NULL COMMENT '城市',
  cost_ms bigint NULL COMMENT '耗时毫秒',
  deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_by bigint NULL, create_time datetime NULL, update_by bigint NULL, update_time datetime NULL,
  PRIMARY KEY (id),
  KEY idx_oper_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志（V6.4 补齐种子）';

CREATE TABLE IF NOT EXISTS sys_role (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_name varchar(64) NOT NULL COMMENT '角色名',
  role_key varchar(64) NOT NULL COMMENT '角色标识（admin 直通）',
  remark varchar(255) NULL COMMENT '备注',
  create_time datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS sys_menu (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  parent_id bigint NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  name varchar(64) NOT NULL COMMENT '名称',
  path varchar(128) NULL COMMENT '路由路径',
  component varchar(128) NULL COMMENT '组件路径',
  perms varchar(128) NULL COMMENT '权限标识（如 demo_fee:list）',
  type tinyint NOT NULL DEFAULT 1 COMMENT '类型：1目录2菜单3按钮',
  sort int NOT NULL DEFAULT 0 COMMENT '排序',
  status tinyint NOT NULL DEFAULT 1 COMMENT '状态：1启用0停用',
  deleted tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单与权限点';

CREATE TABLE IF NOT EXISTS sys_user_role (
  user_id bigint NOT NULL COMMENT '用户ID',
  role_id bigint NOT NULL COMMENT '角色ID',
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_role_menu (
  role_id bigint NOT NULL COMMENT '角色ID',
  menu_id bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单（perms 随菜单授予）';

INSERT IGNORE INTO sys_user (username, password, password_algo, nickname)
VALUES ('admin', '$2a$10$BQz.XdhOV7sJOpH9z/BeheARd01UQKdchBsEgPfa.rYgmwTh75PaO', 'bcrypt', '管理员');

INSERT IGNORE INTO sys_role (id, role_name, role_key, remark)
VALUES (1, '超级管理员', 'admin', '内置角色：接口权限点直通');
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, 1 FROM sys_user u WHERE u.username = 'admin';
