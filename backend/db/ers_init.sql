-- =========================================
-- 河南科技学院入校登记系统 - 数据库初始化扩展
-- =========================================

-- 1. 修改 sys_user 表，增加 ERS 项目所需字段
ALTER TABLE `sys_user` ADD COLUMN `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名' AFTER `mobile`;
ALTER TABLE `sys_user` ADD COLUMN `source` tinyint DEFAULT 0 COMMENT '来源 0直接创建 1访客提升' AFTER `real_name`;
ALTER TABLE `sys_user` ADD COLUMN `source_visitor_id` bigint DEFAULT NULL COMMENT '来源访客ID' AFTER `source`;
ALTER TABLE `sys_user` ADD COLUMN `promote_time` datetime DEFAULT NULL COMMENT '提升时间' AFTER `source_visitor_id`;

-- 2. 初始化菜单数据 - 管理员管理模块 (ERS 均衡版)
-- 先查询最大menu_id，确保不冲突
-- 假设当前最大menu_id为30，则从31开始插入

-- 管理员管理一级菜单
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`)
VALUES (100, 1, '管理员管理', 'sys/manager', 'sys:manager:list', 1, 'admin', 1);

-- 管理员管理二级菜单（按钮权限）
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (101, 100, '查看', 'sys:manager:list,sys:manager:info', 2, NULL, 0);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (102, 100, '新增', 'sys:manager:save', 2, NULL, 0);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (103, 100, '修改', 'sys:manager:update', 2, NULL, 0);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (104, 100, '删除', 'sys:manager:delete', 2, NULL, 0);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (105, 100, '启用禁用', 'sys:manager:update', 2, NULL, 0);

-- 更新超级管理员的 real_name
UPDATE `sys_user` SET `real_name` = '超级管理员' WHERE `user_id` = 1;
