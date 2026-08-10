-- 入校记录表
DROP TABLE IF EXISTS ers_entry_record;
CREATE TABLE ers_entry_record (
    record_id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '入校记录ID',
    record_no              VARCHAR(50) NOT NULL COMMENT '记录编号',
    application_id         BIGINT NOT NULL COMMENT '申请ID',
    visitor_id             BIGINT NOT NULL COMMENT '访客ID',
    visitor_name           VARCHAR(50) NOT NULL COMMENT '访客姓名',
    phone                  VARCHAR(20) NOT NULL COMMENT '手机号',
    entry_date             DATE NOT NULL COMMENT '预约日期',
    entry_start_time       DATETIME NOT NULL COMMENT '预约开始时间',
    entry_end_time         DATETIME NOT NULL COMMENT '预约结束时间',
    actual_entry_time      DATETIME DEFAULT NULL COMMENT '实际入校时间',
    verify_status          TINYINT NOT NULL DEFAULT 0 COMMENT '核销状态 0未核销 1已核销',
    verify_gate            VARCHAR(100) DEFAULT NULL COMMENT '核验地点/校门',
    verify_user_id         BIGINT DEFAULT NULL COMMENT '核验人(sys_user.user_id)',
    qr_code_content        TEXT DEFAULT NULL COMMENT '二维码内容',
    record_status          TINYINT NOT NULL DEFAULT 0 COMMENT '记录状态 0待入校 1已入校 2已过期 3已爽约 4已完成',
    remark                 VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_record_no (record_no),
    UNIQUE KEY uk_application_id (application_id),
    KEY idx_visitor_id (visitor_id),
    KEY idx_entry_date (entry_date),
    KEY idx_verify_status (verify_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入校记录表';

-- 初始化菜单数据 - 监控统计模块
INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`)
VALUES (300, 1, '监控统计', NULL, NULL, 0, 'monitor', 6);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`)
VALUES (301, 300, '今日概览', 'ers/monitor/todayOverview', 'ers:monitor:todayOverview', 1, 'detail', 1);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `url`, `perms`, `type`, `icon`, `order_num`)
VALUES (302, 300, '入校记录', 'ers/record/list', 'ers:record:list', 1, 'log', 2);

INSERT INTO `sys_menu` (`menu_id`, `parent_id`, `name`, `perms`, `type`, `icon`, `order_num`)
VALUES (303, 302, '导出', 'ers:record:export', 2, NULL, 0);

-- 初始化时间配置数据
INSERT INTO ers_time_slot (slot_name, start_time, end_time, max_count, current_count, status, sort, remark)
VALUES
('上午时段', '08:00:00', '12:00:00', 100, 0, 1, 1, '默认上午入校时段'),
('下午时段', '14:00:00', '18:00:00', 100, 0, 1, 2, '默认下午入校时段');
