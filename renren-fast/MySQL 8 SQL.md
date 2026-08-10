可以，按你现在这个 **renren-fast 现有数据库** 来看，**不需要新建数据库**，直接在当前库里新增“入校登记系统”的业务表就行；后台管理员、角色、菜单、日志这些继续复用 renren-fast 自带的 `sys_user`、`sys_role`、`sys_user_role`、`sys_menu`、`sys_log`、`sys_config`。而业务侧再补充访客、申请、审批、记录、黑名单、爽约、时间段、特殊日期这些表，正好符合你 SPEC 里的需求范围。

下面我直接给你一版**可落库的 MySQL 8 SQL**。
这版方案默认：

- **管理员**：复用 `sys_user`
- **角色权限**：复用 `sys_role`、`sys_user_role`
- **访客用户**：单独新建 `ers_visitor`
- **业务表**：统一用 `ers_` 前缀，避免和 renren-fast 原表冲突

------

# 一、直接执行的数据库代码

> 使用前先切到你当前的 renren-fast 数据库

```sql
USE renren_fast;
```

------

## 1. 访客表 `ers_visitor`

```sql
DROP TABLE IF EXISTS ers_visitor;
CREATE TABLE ers_visitor (
    visitor_id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '访客ID',
    account               VARCHAR(50) NOT NULL COMMENT '登录账号',
    password              VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    real_name             VARCHAR(50) NOT NULL COMMENT '真实姓名',
    gender                TINYINT DEFAULT NULL COMMENT '性别 0男 1女',
    phone                 VARCHAR(20) NOT NULL COMMENT '手机号',
    email                 VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    id_card               VARCHAR(255) DEFAULT NULL COMMENT '身份证号(建议加密存储)',
    avatar                VARCHAR(255) DEFAULT NULL COMMENT '头像',
    auth_status           TINYINT NOT NULL DEFAULT 0 COMMENT '实名认证状态 0未认证 1已认证',
    status                TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
    blacklist_status      TINYINT NOT NULL DEFAULT 0 COMMENT '黑名单状态 0否 1是',
    promoted_to_admin     TINYINT NOT NULL DEFAULT 0 COMMENT '是否已提升为管理员 0否 1是',
    no_show_count         INT NOT NULL DEFAULT 0 COMMENT '累计爽约次数',
    last_login_time       DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip         VARCHAR(64) DEFAULT NULL COMMENT '最后登录IP',
    remark                VARCHAR(255) DEFAULT NULL COMMENT '备注',
    deleted               TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    create_by             BIGINT DEFAULT NULL COMMENT '创建人',
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by             BIGINT DEFAULT NULL COMMENT '更新人',
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (visitor_id),
    UNIQUE KEY uk_visitor_account (account),
    UNIQUE KEY uk_visitor_phone (phone),
    KEY idx_visitor_real_name (real_name),
    KEY idx_visitor_status (status),
    KEY idx_visitor_blacklist_status (blacklist_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客表';
```

------

## 2. 时间段配置表 `ers_time_slot`

```sql
DROP TABLE IF EXISTS ers_time_slot;
CREATE TABLE ers_time_slot (
    slot_id               BIGINT NOT NULL AUTO_INCREMENT COMMENT '时间段ID',
    slot_name             VARCHAR(50) NOT NULL COMMENT '时间段名称',
    start_time            TIME NOT NULL COMMENT '开始时间',
    end_time              TIME NOT NULL COMMENT '结束时间',
    max_count             INT NOT NULL DEFAULT 0 COMMENT '最大预约人数',
    current_count         INT NOT NULL DEFAULT 0 COMMENT '当前预约人数',
    status                TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
    sort                  INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark                VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_by             BIGINT DEFAULT NULL COMMENT '创建人(sys_user.user_id)',
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by             BIGINT DEFAULT NULL COMMENT '更新人(sys_user.user_id)',
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (slot_id),
    KEY idx_slot_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入校时间段配置表';
```

------

## 3. 特殊日期配置表 `ers_special_date`

```sql
DROP TABLE IF EXISTS ers_special_date;
CREATE TABLE ers_special_date (
    special_date_id       BIGINT NOT NULL AUTO_INCREMENT COMMENT '特殊日期ID',
<<<<<<< HEAD
    start_date            VARCHAR(50) COMMENT '开始日期',
    end_date              VARCHAR(50) COMMENT '结束日期',
    start_time            VARCHAR(50) COMMENT '开始时间',
    end_time              VARCHAR(50) COMMENT '结束时间',
    remark                VARCHAR(255) DEFAULT NULL COMMENT '备注',
    status                TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0无效 1有效',
    create_by             BIGINT DEFAULT NULL COMMENT '创建人',
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by             BIGINT DEFAULT NULL COMMENT '更新人',
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (special_date_id)
=======
    special_date          DATE NOT NULL COMMENT '特殊日期',
    date_type             TINYINT NOT NULL COMMENT '日期类型 1节假日 2闭校日',
    remark                VARCHAR(255) DEFAULT NULL COMMENT '备注',
    status                TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0无效 1有效',
    create_by             BIGINT DEFAULT NULL COMMENT '创建人(sys_user.user_id)',
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by             BIGINT DEFAULT NULL COMMENT '更新人(sys_user.user_id)',
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (special_date_id),
    UNIQUE KEY uk_special_date (special_date),
    KEY idx_date_type (date_type)
>>>>>>> together_plus
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特殊日期配置表';
```

------

## 4. 入校申请表 `ers_entry_application`

这个表对应申请提交、列表、详情、取消、审批状态流转。状态值按 SPEC 设计：
`0待审批 1已通过 2已拒绝 3已取消 4已爽约 5已完成`。

```sql
DROP TABLE IF EXISTS ers_entry_application;
CREATE TABLE ers_entry_application (
    application_id        BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    application_no        VARCHAR(50) NOT NULL COMMENT '申请编号',
    visitor_id            BIGINT NOT NULL COMMENT '访客ID',
    visitor_name          VARCHAR(50) NOT NULL COMMENT '访客姓名(冗余)',
    phone                 VARCHAR(20) NOT NULL COMMENT '访客手机号(冗余)',
    id_card               VARCHAR(255) DEFAULT NULL COMMENT '身份证号(冗余/加密)',
    entry_date            DATE NOT NULL COMMENT '预约入校日期',
    slot_id               BIGINT NOT NULL COMMENT '时间段ID',
    entry_start_time      DATETIME NOT NULL COMMENT '预约开始时间',
    entry_end_time        DATETIME NOT NULL COMMENT '预约结束时间',
    reason                VARCHAR(500) NOT NULL COMMENT '入校事由',
    visit_unit            VARCHAR(100) DEFAULT NULL COMMENT '到访单位/部门',
    companion_count       INT NOT NULL DEFAULT 0 COMMENT '陪同人数',
    attachment_url        VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
    status                TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0待审批 1已通过 2已拒绝 3已取消 4已爽约 5已完成',
    approval_user_id      BIGINT DEFAULT NULL COMMENT '审批人(sys_user.user_id)',
    approval_time         DATETIME DEFAULT NULL COMMENT '审批时间',
    approval_remark       VARCHAR(500) DEFAULT NULL COMMENT '审批意见/拒绝原因',
    cancel_time           DATETIME DEFAULT NULL COMMENT '取消时间',
    cancel_reason         VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
    record_no             VARCHAR(50) DEFAULT NULL COMMENT '通过后生成的记录编号',
    qr_code_content       TEXT DEFAULT NULL COMMENT '二维码内容',
    deleted               TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除 0否 1是',
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (application_id),
    UNIQUE KEY uk_application_no (application_no),
    UNIQUE KEY uk_record_no (record_no),
    KEY idx_visitor_id (visitor_id),
    KEY idx_status (status),
    KEY idx_entry_date (entry_date),
    KEY idx_slot_id (slot_id),
    KEY idx_approval_user_id (approval_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入校申请表';
```

------

## 5. 审批记录表 `ers_entry_approval`

```sql
DROP TABLE IF EXISTS ers_entry_approval;
CREATE TABLE ers_entry_approval (
    approval_id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '审批记录ID',
    application_id        BIGINT NOT NULL COMMENT '申请ID',
    approval_user_id      BIGINT NOT NULL COMMENT '审批人(sys_user.user_id)',
    approval_action       TINYINT NOT NULL COMMENT '审批动作 1通过 2拒绝',
    approval_remark       VARCHAR(500) DEFAULT NULL COMMENT '审批意见/拒绝原因',
    approval_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    PRIMARY KEY (approval_id),
    KEY idx_application_id (application_id),
    KEY idx_approval_user_id (approval_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申请审批记录表';
```

------

## 6. 入校记录表 `ers_entry_record`

审批通过后生成，用于扫码核验、实际入校记录。

```sql
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
```

------

## 7. 黑名单表 `ers_blacklist`

```sql
DROP TABLE IF EXISTS ers_blacklist;
CREATE TABLE ers_blacklist (
    blacklist_id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '黑名单ID',
    visitor_id             BIGINT NOT NULL COMMENT '访客ID',
    reason                 VARCHAR(255) NOT NULL COMMENT '拉黑原因',
    source_type            TINYINT NOT NULL DEFAULT 0 COMMENT '来源 0手动拉黑 1爽约触发',
    start_time             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
    end_time               DATETIME DEFAULT NULL COMMENT '结束时间 NULL表示永久',
    status                 TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0已解除 1生效中 2已过期',
    operate_user_id        BIGINT DEFAULT NULL COMMENT '操作人(sys_user.user_id)',
    release_user_id        BIGINT DEFAULT NULL COMMENT '解除人(sys_user.user_id)',
    release_time           DATETIME DEFAULT NULL COMMENT '解除时间',
    release_reason         VARCHAR(255) DEFAULT NULL COMMENT '解除原因',
    create_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (blacklist_id),
    KEY idx_visitor_id (visitor_id),
    KEY idx_status (status),
    KEY idx_end_time (end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='黑名单表';
```

------

## 8. 爽约记录表 `ers_no_show_record`

```sql
DROP TABLE IF EXISTS ers_no_show_record;
CREATE TABLE ers_no_show_record (
    no_show_id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '爽约记录ID',
    visitor_id             BIGINT NOT NULL COMMENT '访客ID',
    application_id         BIGINT NOT NULL COMMENT '申请ID',
    record_id              BIGINT DEFAULT NULL COMMENT '入校记录ID',
    no_show_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '爽约判定时间',
    late_minutes           INT NOT NULL DEFAULT 0 COMMENT '迟到分钟数',
    trigger_blacklist      TINYINT NOT NULL DEFAULT 0 COMMENT '是否触发黑名单 0否 1是',
    remark                 VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (no_show_id),
    KEY idx_visitor_id (visitor_id),
    KEY idx_application_id (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爽约记录表';
```

------

## 9. Token 黑名单表 `ers_token_blacklist`

如果你不想继续只用 renren-fast 默认 token 表，这张表可以补上，专门处理退出登录后的失效 token。SPEC 里明确要求支持 token 黑名单机制。

```sql
DROP TABLE IF EXISTS ers_token_blacklist;
CREATE TABLE ers_token_blacklist (
    id                    BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    token                 VARCHAR(512) NOT NULL COMMENT '失效Token',
    expire_time           DATETIME NOT NULL COMMENT 'Token过期时间',
    user_type             TINYINT DEFAULT NULL COMMENT '用户类型 0访客 1管理员',
    user_id               BIGINT DEFAULT NULL COMMENT '用户ID',
    create_time           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入黑名单时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token(255)),
    KEY idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Token黑名单表';
```

------

## 10. 登录日志表 `ers_login_log`

```sql
DROP TABLE IF EXISTS ers_login_log;
CREATE TABLE ers_login_log (
    log_id                 BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_type              TINYINT NOT NULL COMMENT '用户类型 0访客 1管理员',
    user_id                BIGINT DEFAULT NULL COMMENT '用户ID',
    account                VARCHAR(50) NOT NULL COMMENT '登录账号',
    login_result           TINYINT NOT NULL COMMENT '登录结果 0失败 1成功',
    fail_reason            VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
    login_ip               VARCHAR(64) DEFAULT NULL COMMENT '登录IP',
    user_agent             VARCHAR(500) DEFAULT NULL COMMENT '客户端信息',
    login_time             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    PRIMARY KEY (log_id),
    KEY idx_user_type_user_id (user_type, user_id),
    KEY idx_account (account),
    KEY idx_login_time (login_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
```

------

## 11. 业务操作日志表 `ers_operation_log`

虽然 renren-fast 已有 `sys_log`，但业务操作单独分出来更清晰，比如审批、拉黑、核销这些。

```sql
DROP TABLE IF EXISTS ers_operation_log;
CREATE TABLE ers_operation_log (
    log_id                 BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_type              TINYINT NOT NULL COMMENT '用户类型 0访客 1管理员',
    user_id                BIGINT DEFAULT NULL COMMENT '操作人ID',
    user_name              VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
    module_name            VARCHAR(50) NOT NULL COMMENT '模块名称',
    operation_type         VARCHAR(50) NOT NULL COMMENT '操作类型',
    business_id            BIGINT DEFAULT NULL COMMENT '业务ID',
    request_method         VARCHAR(10) DEFAULT NULL COMMENT '请求方式',
    request_uri            VARCHAR(255) DEFAULT NULL COMMENT '请求地址',
    request_param          TEXT DEFAULT NULL COMMENT '请求参数',
    response_data          TEXT DEFAULT NULL COMMENT '响应数据',
    operation_result       TINYINT NOT NULL DEFAULT 1 COMMENT '操作结果 0失败 1成功',
    error_msg              VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    operation_ip           VARCHAR(64) DEFAULT NULL COMMENT '操作IP',
    operation_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (log_id),
    KEY idx_user_type_user_id (user_type, user_id),
    KEY idx_module_name (module_name),
    KEY idx_business_id (business_id),
    KEY idx_operation_time (operation_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务操作日志表';
```

------

## 12. 系统业务配置表 `ers_config`

这个表专门放你的业务配置，比如爽约规则。
renren-fast 原本有 `sys_config`，但为了避免和平台配置混在一起，我建议你单独建一张业务配置表。

```sql
DROP TABLE IF EXISTS ers_config;
CREATE TABLE ers_config (
    config_id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    config_key             VARCHAR(100) NOT NULL COMMENT '配置键',
    config_name            VARCHAR(100) NOT NULL COMMENT '配置名称',
    config_value           TEXT NOT NULL COMMENT '配置值',
    value_type             VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型 string/json/number/boolean',
    remark                 VARCHAR(255) DEFAULT NULL COMMENT '备注',
    status                 TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_by              BIGINT DEFAULT NULL COMMENT '创建人(sys_user.user_id)',
    create_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by              BIGINT DEFAULT NULL COMMENT '更新人(sys_user.user_id)',
    update_time            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (config_id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入校登记系统业务配置表';
```

------

# 二、初始化数据

## 1. 初始化时间段

```sql
INSERT INTO ers_time_slot (slot_name, start_time, end_time, max_count, current_count, status, sort, remark)
VALUES
('上午时段', '08:00:00', '12:00:00', 100, 0, 1, 1, '默认上午入校时段'),
('下午时段', '14:00:00', '18:00:00', 100, 0, 1, 2, '默认下午入校时段');
```

------

## 2. 初始化爽约规则

SPEC 中提到迟到分钟数、爽约次数限制、黑名单天数等配置。

```sql
INSERT INTO ers_config (config_key, config_name, config_value, value_type, remark, status)
VALUES
(
    'NO_SHOW_RULE',
    '爽约规则',
    '{"lateMinutes":30,"maxNoShowCount":3,"blacklistDays":30}',
    'json',
    '迟到30分钟计爽约，累计3次进入黑名单30天',
    1
);
```

------

## 3. 初始化特殊日期示例

```sql
INSERT INTO ers_special_date (special_date, date_type, remark, status)
VALUES
('2026-01-01', 1, '元旦节假日', 1),
('2026-02-10', 2, '学校闭校日', 1);
```

------

# 三、为什么我这样设计

结合你现在的 renren-fast 库，这样最合适：

## 1. 不重建管理员体系

你截图里已经有：

- `sys_user`
- `sys_role`
- `sys_user_role`
- `sys_menu`
- `sys_log`
- `sys_config`

这些就是 renren-fast 的后台基础表。
所以管理员审批端直接复用它们，不要再单独建 `sys_admin` 了。

## 2. 访客和管理员分开

访客不是后台管理员，业务差别大：

- 访客要注册、实名认证、申请入校
- 管理员要审批、配置时间段、处理黑名单

所以单独建 `ers_visitor` 最清晰。

## 3. 业务表统一 `ers_` 前缀

这样你一眼就能看出：

- 哪些是 renren-fast 原表
- 哪些是你项目新增表

后期写 Mapper、Entity、菜单权限时也更清楚。

------

# 四、你实际开发时怎么对应

## 复用 renren-fast 原表

- 管理员登录：`sys_user`
- 权限角色：`sys_role`、`sys_user_role`
- 菜单权限：`sys_menu`
- 平台日志：`sys_log`

## 新增业务表

- 访客：`ers_visitor`
- 申请：`ers_entry_application`
- 审批流水：`ers_entry_approval`
- 入校记录：`ers_entry_record`
- 黑名单：`ers_blacklist`
- 爽约：`ers_no_show_record`
- 时间段：`ers_time_slot`
- 特殊日期：`ers_special_date`
- 业务配置：`ers_config`
- 登录日志：`ers_login_log`
- token黑名单：`ers_token_blacklist`
- 业务日志：`ers_operation_log`

------

# 五、给你的一个直接建议

你现在可以分两步走：

### 第一步

先把我上面的 SQL 全部执行，表建出来。

### 第二步

后端代码优先做这几个模块：

1. `ers_visitor` 注册/登录
2. `ers_entry_application` 提交申请
3. `ers_entry_approval` 审批通过/拒绝
4. `ers_entry_record` 核销入校
5. `ers_blacklist` + `ers_no_show_record` 爽约拉黑

这就是主链路。

------

如果你愿意，我下一条可以直接继续给你：

**“和这套数据库一一对应的 Java 实体类代码（entity）”**
我会按 renren-fast + MyBatis-Plus 风格直接写。