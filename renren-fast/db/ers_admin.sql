-- =========================================
-- 河南科技学院入校登记系统 - 管理员独立模块
-- =========================================

-- 管理员表
DROP TABLE IF EXISTS `ers_admin`;
CREATE TABLE `ers_admin` (
  `admin_id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `salt` varchar(20) NOT NULL COMMENT '盐',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint DEFAULT 1 COMMENT '状态 0禁用 1正常',
  `source` tinyint DEFAULT 0 COMMENT '来源 0直接创建 1访客提升',
  `source_visitor_id` bigint DEFAULT NULL COMMENT '来源访客ID',
  `promote_time` datetime DEFAULT NULL COMMENT '提升时间',
  `create_user_id` bigint DEFAULT NULL COMMENT '创建者ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`admin_id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COMMENT='管理员表';

-- 管理员角色关联表
DROP TABLE IF EXISTS `ers_admin_role`;
CREATE TABLE `ers_admin_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_admin_id` (`admin_id`)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COMMENT='管理员角色关联表';

-- 初始化菜单数据
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`)
VALUES (200, 1, '管理员管理', 'sys/manager', 'sys:manager:list', 1, 'admin', 1);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (201, 200, '查看', 'sys:manager:list,sys:manager:info', 2, NULL, 0);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (202, 200, '新增', 'sys:manager:save', 2, NULL, 0);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (203, 200, '修改', 'sys:manager:update', 2, NULL, 0);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (204, 200, '删除', 'sys:manager:delete', 2, NULL, 0);

-- 初始化一个管理员 (admin/admin)
INSERT INTO `ers_admin` (`admin_id`, `username`, `password`, `salt`, `real_name`, `status`, `create_time`)
VALUES (1, 'admin', '9ec9750e709431dad22365cabc5c625482e574c74adaebba7dd02f1129e4ce1d', 'YzcmCZNvbXocrsz9dm8e', '超级管理员', 1, NOW());
