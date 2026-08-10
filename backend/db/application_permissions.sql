-- =============================================
-- 入校审批模块权限配置
-- 使用说明：在数据库中执行此SQL文件
-- =============================================

-- 1. 检查并创建application表
-- 如果表不存在则创建
CREATE TABLE IF NOT EXISTS `application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `real_name` varchar(50) NOT NULL COMMENT '访客姓名',
  `phone` varchar(20) NOT NULL COMMENT '访客手机号',
  `id_card` varchar(18) COMMENT '身份证号',
  `reason` varchar(500) COMMENT '入校事由',
  `appointment_time` datetime COMMENT '预约入校时间',
  `status` tinyint DEFAULT 0 COMMENT '申请状态：0-待审批 1-已通过 2-已拒绝 3-已取消 4-已预约 5-已完成',
  `reject_reason` varchar(500) COMMENT '拒绝原因',
  `entry_code` varchar(50) COMMENT '入校编号',
  `qr_code_path` varchar(200) COMMENT '二维码路径',
  `approver_id` bigint COMMENT '审批人ID',
  `approve_time` datetime COMMENT '审批时间',
  `create_user_id` bigint COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_appointment_time` (`appointment_time`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=`InnoDB` DEFAULT CHARACTER SET utf8mb4 COMMENT='入校申请';

-- 2. 插入入校审批菜单权限
-- 先检查menu_id是否已存在
DELETE FROM `sys_menu` WHERE `menu_id` IN (31, 32, 33, 34, 35, 36, 37);

INSERT INTO `sys_menu`(`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`) VALUES 
(31, 0, '入校审批', NULL, NULL, 0, 'form', 0),
(32, 31, '入校申请管理', 'application/application', NULL, 1, 'application', 1),
(33, 32, '查看', NULL, 'application:list,application:detail', 2, NULL, 0),
(34, 32, '审批通过', NULL, 'application:approve', 2, NULL, 0),
(35, 32, '审批拒绝', NULL, 'application:reject', 2, NULL, 0),
(36, 32, '批量审批', NULL, 'application:batchApprove', 2, NULL, 0),
(37, 32, '批量删除', NULL, 'application:batchDelete', 2, NULL, 0);

-- 3. 为管理员角色（role_id=1）分配入校审批权限
-- 先检查role_menu表中是否已存在这些权限
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (31, 32, 33, 34, 35, 36, 37);

INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id IN (31, 32, 33, 34, 35, 36, 37);

-- 4. 插入测试数据
INSERT INTO `application` (`real_name`, `phone`, `id_card`, `reason`, `appointment_time`, `status`, `create_user_id`, `create_time`) VALUES
('张三', '13800138001', '110101199001011234', '业务洽谈', '2026-04-23 09:00:00', 0, 1, NOW()),
('李四', '13800138002', '110101199002022345', '技术交流', '2026-04-24 14:00:00', 0, 1, NOW()),
('王五', '13800138003', '110101199003033456', '会议参加', '2026-04-25 10:00:00', 0, 1, NOW()),
('赵六', '13800138004', '110101199004044567', '参观学习', '2026-04-26 15:00:00', 0, 1, NOW()),
('钱七', '13800138005', '110101199005055678', '面试', '2026-04-27 10:00:00', 0, 1, NOW());

-- 5. 验证权限配置
SELECT '权限配置完成！' AS message;
SELECT '菜单配置：' AS info;
SELECT menu_id, parent_id, name, perms FROM sys_menu WHERE menu_id BETWEEN 31 AND 37;
SELECT '角色权限：' AS info;
SELECT rm.role_id, r.role_name, m.name AS menu_name, m.perms 
FROM sys_role_menu rm
LEFT JOIN sys_role r ON rm.role_id = r.role_id
LEFT JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE rm.menu_id BETWEEN 31 AND 37;
SELECT '测试数据：' AS info;
SELECT id, real_name, phone, status, create_time FROM application;
