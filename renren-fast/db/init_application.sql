-- =============================================
-- 入校审批模块 - 简单初始化脚本
-- 请在MySQL中直接执行此文件
-- =============================================

USE renren_fast;

-- 创建入校申请表
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
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COMMENT='入校申请';

-- 插入测试数据
INSERT INTO `application` (`real_name`, `phone`, `id_card`, `reason`, `appointment_time`, `status`, `create_user_id`) VALUES
('张三', '13800138001', '110101199001011234', '业务洽谈', '2026-04-23 09:00:00', 0, 1),
('李四', '13800138002', '110101199002022345', '技术交流', '2026-04-24 14:00:00', 0, 1),
('王五', '13800138003', '110101199003033456', '会议参加', '2026-04-25 10:00:00', 0, 1),
('赵六', '13800138004', '110101199004044567', '参观学习', '2026-04-26 15:00:00', 0, 1),
('钱七', '13800138005', '110101199005055678', '面试', '2026-04-27 10:00:00', 0, 1);

-- 验证
SELECT '初始化完成！' AS message;
SELECT * FROM application;
