-- =====================================================
-- 河南科技学院入校登记系统 (ERS) 数据库脚本
-- 数据库: renren_fast
-- 创建时间: 2026-04-23
-- =====================================================

USE renren_fast;

-- =====================================================
-- 1. 入校时间段配置表
-- =====================================================
DROP TABLE IF EXISTS ers_time_slot;
CREATE TABLE ers_time_slot (
    slot_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '时间段ID',
    slot_name VARCHAR(50) NOT NULL COMMENT '时间段名称',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    max_count INT NOT NULL DEFAULT 0 COMMENT '最大预约人数',
    current_count INT NOT NULL DEFAULT 0 COMMENT '当前预约人数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (slot_id),
    KEY idx_slot_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入校时间段配置表';

-- =====================================================
-- 2. 特殊日期配置表
-- =====================================================
DROP TABLE IF EXISTS ers_special_date;
CREATE TABLE ers_special_date (
    special_date_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '特殊日期ID',
    start_date VARCHAR(50) NOT NULL COMMENT '开始日期',
    end_date VARCHAR(50) NOT NULL COMMENT '结束日期',
    start_time VARCHAR(50) COMMENT '开始时间',
    end_time VARCHAR(50) COMMENT '结束时间',
    date_type TINYINT NOT NULL COMMENT '日期类型 1节假日 2闭校日',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0无效 1有效',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (special_date_id),
    KEY idx_date_type (date_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特殊日期配置表';

-- =====================================================
-- 3. 系统业务配置表
-- =====================================================
DROP TABLE IF EXISTS ers_config;
CREATE TABLE ers_config (
    config_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_value TEXT NOT NULL COMMENT '配置值',
    value_type VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型 string/json/number/boolean',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入校登记系统业务配置表';

-- =====================================================
-- 4. 初始化默认数据
-- =====================================================

-- 初始化时间段
INSERT INTO ers_time_slot (slot_name, start_time, end_time, max_count, current_count, status, sort, remark) VALUES
('上午时段', '08:00:00', '12:00:00', 100, 0, 1, 1, '默认上午入校时段'),
('下午时段', '14:00:00', '18:00:00', 100, 0, 1, 2, '默认下午入校时段');

-- 初始化爽约规则
INSERT INTO ers_config (config_key, config_name, config_value, value_type, remark, status) VALUES
('NO_SHOW_RULE', '爽约规则', '{"lateMinutes":30,"maxNoShowCount":3,"blacklistDays":30}', 'json', '迟到30分钟计爽约，累计3次进入黑名单30天', 1);

-- 初始化特殊日期示例
INSERT INTO ers_special_date (start_date, end_date, start_time, end_time, date_type, remark, status) VALUES
('2026-05-01', '2026-05-03', '09:00', '12:00', 1, '五一假期', 1),
('2026-06-01', '2026-06-01', NULL, NULL, 1, '端午节', 1);

-- =====================================================
-- 5. 入校申请表
-- =====================================================
DROP TABLE IF EXISTS ers_entry_application;
CREATE TABLE ers_entry_application (
    application_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    application_no VARCHAR(50) NOT NULL COMMENT '申请编号',
    visitor_id BIGINT NOT NULL COMMENT '访客ID',
    visitor_name VARCHAR(50) NOT NULL COMMENT '访客姓名(冗余)',
    phone VARCHAR(20) NOT NULL COMMENT '访客手机号(冗余)',
    id_card VARCHAR(255) DEFAULT NULL COMMENT '身份证号(冗余/加密)',
    entry_date DATE NOT NULL COMMENT '预约入校日期',
    slot_id BIGINT NOT NULL COMMENT '时间段ID',
    time_slot VARCHAR(50) DEFAULT NULL COMMENT '时间段名称',
    entry_start_time DATETIME NOT NULL COMMENT '预约开始时间',
    entry_end_time DATETIME NOT NULL COMMENT '预约结束时间',
    reason VARCHAR(500) NOT NULL COMMENT '入校事由',
    visit_unit VARCHAR(100) DEFAULT NULL COMMENT '到访单位/部门',
    companion_count INT NOT NULL DEFAULT 0 COMMENT '陪同人数',
    attachment_url VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待审批 1已通过 2已拒绝 3已取消 4已爽约 5已完成',
    approval_user_id BIGINT DEFAULT NULL COMMENT '审批人(sys_user.user_id)',
    approval_time DATETIME DEFAULT NULL COMMENT '审批时间',
    approval_remark VARCHAR(500) DEFAULT NULL COMMENT '审批意见/拒绝原因',
    cancel_time DATETIME DEFAULT NULL COMMENT '取消时间',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    record_no VARCHAR(50) DEFAULT NULL COMMENT '通过后生成的记录编号',
    qr_code_content TEXT DEFAULT NULL COMMENT '二维码内容',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (application_id),
    UNIQUE KEY uk_application_no (application_no),
    UNIQUE KEY uk_record_no (record_no),
    KEY idx_visitor_id (visitor_id),
    KEY idx_status (status),
    KEY idx_entry_date (entry_date),
    KEY idx_slot_id (slot_id),
    KEY idx_approval_user_id (approval_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入校申请表';
