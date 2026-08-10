好的，以下是按重新分配方案生成的完整开发文档，三人各14接口，难度均衡。

---

# 河南科技学院入校登记系统（ERS）- 后端开发文档（均衡版）

> **基于**: renren-fast 人人开源框架  
> **团队**: 3人后端开发（难度均衡：每人14接口）  
> **版本**: v4.0.0（均衡分工版）  
> **日期**: 2026-04-18

---

## 1. 团队分工总览（均衡设计）

| 人员      | 模块               | 接口数 | 核心特点                    | 难度 |
| :-------- | :----------------- | :----: | :-------------------------- | :--: |
| **人员A** | 访客端+个人数据    |   14   | 数据可视化、复杂查询、导出  | ⭐⭐⭐⭐ |
| **人员B** | 审批核心+访客管理  |   14   | 状态机、二维码、批量审批    | ⭐⭐⭐⭐ |
| **人员C** | 统一认证+配置+监控 |   14   | JWT安全、定时任务、实时推送 | ⭐⭐⭐⭐ |

**均衡原则**：
- 接口数量：14=14=14
- 代码行数：~1800行/人
- 技术深度：均有3-4星难点
- 业务重要性：均为核心模块

---

## 2. 人员A：访客端+个人数据（14接口）

> 特点：数据可视化+个人中心，需掌握ECharts数据组装、复杂SQL

### 2.1 用户认证基础（5接口）

| 序号 | 接口                 | 方法 | 功能                  | 技术点     |
| :--: | :------------------- | :--: | :-------------------- | :--------- |
|  1   | `/api/user/register` | POST | 用户注册              | BCrypt加密 |
|  2   | `/api/user/login`    | POST | 用户登录（调C的认证） | 调用C服务  |
|  3   | `/api/user/info`     | GET  | 获取个人信息          | Token解析  |
|  4   | `/api/user/update`   | PUT  | 修改个人信息          | 字段校验   |
|  5   | `/api/user/password` | PUT  | 修改密码              | 旧密码校验 |

### 2.2 个人数据可视化（3接口，核心难点）

| 序号 | 接口                             | 方法 | 功能               | 技术点             |
| :--: | :------------------------------- | :--: | :----------------- | :----------------- |
|  6   | `/api/user/dashboard`            | GET  | **个人数据看板**   | MySQL分组统计      |
|  7   | `/api/user/calendar`             | GET  | **个人入校日历**   | 日期函数、数据填充 |
|  8   | `/api/application/timeline/{id}` | GET  | **申请进度时间轴** | 状态流转记录       |

**接口6：个人数据看板（核心代码）**
```java
/**
 * GET /api/user/dashboard
 * 功能：访客首页数据看板，给ECharts提供图表数据
 * 
 * 返回数据结构：
 * {
 *   "trend": [      // 近6个月申请趋势（折线图）
 *     {"month": "2024-10", "count": 5, "approved": 3, "rejected": 2},
 *     ...
 *   ],
 *   "statusDist": {  // 状态分布（饼图）
 *     "pending": 2, "approved": 10, "rejected": 3, ...
 *   },
 *   "upcoming": {    // 即将入校的申请（卡片）
 *     "date": "2024-04-20", "timeSlot": "09:00-12:00", "recordNo": "ERS..."
 *   },
 *   "stats": {       // 统计数字
 *     "totalApply": 20, "totalEntry": 15, "noShowCount": 2
 *   }
 * }
 */
@GetMapping("/dashboard")
public R dashboard(@RequestHeader("Authorization") String token) {
    Long visitorId = getCurrentUserId(token);
    
    // 【近6个月趋势】使用MySQL DATE_FORMAT分组
    List<Map<String, Object>> trend = applicationService.getBaseMapper()
        .selectMaps(new QueryWrapper<ApplicationEntity>()
            .select("DATE_FORMAT(create_time, '%Y-%m') as month",
                    "count(*) as count",
                    "sum(case when status=1 then 1 else 0 end) as approved",
                    "sum(case when status=2 then 1 else 0 end) as rejected")
            .eq("visitor_id", visitorId)
            .ge("create_time", LocalDate.now().minusMonths(6))
            .groupBy("month")
            .orderByAsc("month")
        );
    
    // 【状态分布】Java内存统计（数据量小，避免多次查询）
    Map<String, Long> statusDist = applicationService.list(
        new QueryWrapper<ApplicationEntity>().eq("visitor_id", visitorId)
    ).stream().collect(Collectors.groupingBy(
        app -> convertStatus(app.getStatus()), 
        Collectors.counting()
    ));
    
    // 【即将入校】取最近一个已通过且未入校的申请
    ApplicationEntity upcoming = applicationService.getOne(
        new QueryWrapper<ApplicationEntity>()
            .eq("visitor_id", visitorId)
            .eq("status", 1)  // 已通过
            .ge("visit_date", LocalDate.now())
            .orderByAsc("visit_date", "time_slot")
            .last("limit 1")
    );
    
    // 【累计统计】
    Map<String, Integer> stats = new HashMap<>();
    stats.put("totalApply", applicationService.count(
        new QueryWrapper<ApplicationEntity>().eq("visitor_id", visitorId)
    ));
    stats.put("totalEntry", entryRecordService.count(
        new QueryWrapper<EntryRecordEntity>()
            .eq("visitor_id", visitorId)
            .eq("status", 2)  // 已离校=完成
    ));
    stats.put("noShowCount", noShowLogService.count(
        new QueryWrapper<NoShowLogEntity>().eq("visitor_id", visitorId)
    ));
    
    return R.ok()
        .put("trend", trend)
        .put("statusDist", statusDist)
        .put("upcoming", upcoming)
        .put("stats", stats);
}
```

接口7：个人入校日历**

```java
/**
 * GET /api/user/calendar?month=2024-04
 * 功能：按月展示哪些日期有入校安排（类似签到日历）
 * 
 * 返回：该月所有日期，有申请的标记状态和时间段
 */
@GetMapping("/calendar")
public R calendar(@RequestParam String month,  // 格式：2024-04
                  @RequestHeader("Authorization") String token) {
    Long visitorId = getCurrentUserId(token);
    
    // 解析月份起止
    LocalDate start = LocalDate.parse(month + "-01");
    LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
    
    // 查询该月所有申请
    List<ApplicationEntity> list = applicationService.list(
        new QueryWrapper<ApplicationEntity>()
            .eq("visitor_id", visitorId)
            .between("visit_date", start, end)
    );
    
    // 按日期分组
    Map<LocalDate, List<CalendarVO>> calendarMap = list.stream()
        .collect(Collectors.groupingBy(
            ApplicationEntity::getVisitDate,
            Collectors.mapping(app -> {
                CalendarVO vo = new CalendarVO();
                vo.setId(app.getId());
                vo.setTimeSlot(app.getTimeSlot());
                vo.setStatus(app.getStatus());
                vo.setStatusText(convertStatus(app.getStatus()));
                vo.setHasQrCode(app.getStatus() == 1);  // 已通过显示二维码
                return vo;
            }, Collectors.toList())
        ));
    
    // 填充整月日期（包括无申请的日期）
    List<Map<String, Object>> result = new ArrayList<>();
    for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
        Map<String, Object> day = new HashMap<>();
        day.put("date", date);
        day.put("dayOfWeek", date.getDayOfWeek().getValue());  // 1=周一
        day.put("items", calendarMap.getOrDefault(date, Collections.emptyList()));
        result.add(day);
    }
    
    return R.ok().put("calendar", result);
}
```

### 2.3 入校申请（4接口）

| 序号 | 接口                           | 方法 | 功能         | 技术点   |
| :--: | :----------------------------- | :--: | :----------- | :------- |
|  9   | `/api/application/submit`      | POST | 提交入校申请 | 名额检查 |
|  10  | `/api/application/list`        | GET  | 申请列表     | 分页     |
|  11  | `/api/application/detail/{id}` | GET  | 申请详情     | 权限校验 |
|  12  | `/api/application/cancel/{id}` | PUT  | 取消申请     | 状态校验 |

### 2.4 记录与导出（2接口）

| 序号 | 接口                    | 方法 | 功能             | 技术点    |
| :--: | :---------------------- | :--: | :--------------- | :-------- |
|  13  | `/api/record/my`        | GET  | 我的入校记录     | 分页      |
|  14  | `/admin/visitor/export` | GET  | **访客数据导出** | EasyExcel |

**接口14：访客导出（从B移来，A负责访客数据）**
```java
/**
 * GET /admin/visitor/export?status=0&startDate=2024-01-01
 * 功能：管理员导出访客列表（Excel）
 * 
 * 注意：虽然路径是/admin，但A负责实现，因为A熟悉访客表结构
 * B审批时需要导出访客信息，调用此接口
 */
@GetMapping("/export")
public void export(@RequestParam(required = false) Integer status,
                   @RequestParam(required = false) String startDate,
                   HttpServletResponse response) throws IOException {
    
    // 查询数据
    List<VisitorEntity> list = visitorService.list(
        new QueryWrapper<VisitorEntity>()
            .eq(status != null, "status", status)
            .ge(StringUtils.isNotBlank(startDate), "create_time", startDate)
    );
    
    // EasyExcel导出
    List<VisitorExcelVO> excelList = list.stream().map(v -> {
        VisitorExcelVO vo = new VisitorExcelVO();
        BeanUtils.copyProperties(v, vo);
        vo.setStatusText(convertStatus(v.getStatus()));
        return vo;
    }).collect(Collectors.toList());
    
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding("utf-8");
    response.setHeader("Content-disposition", "attachment;filename=visitor_export.xlsx");
    
    EasyExcel.write(response.getOutputStream(), VisitorExcelVO.class)
        .sheet("访客列表")
        .doWrite(excelList);
}
```

---

## 3. 人员B：审批核心+访客管理（14接口）

> 特点：状态机精简，专注审批核心业务，不再负责导出统计

### 3.1 访客管理（6接口）

| 序号 | 接口                           |  方法  | 功能             | 技术点       |
| :--: | :----------------------------- | :----: | :--------------- | :----------- |
|  1   | `/admin/visitor/list`          |  GET   | 访客列表         | 分页、多条件 |
|  2   | `/admin/visitor/detail/{id}`   |  GET   | 访客详情         | 关联查询     |
|  3   | `/admin/visitor/update/{id}`   |  PUT   | 修改访客         | 字段校验     |
|  4   | `/admin/visitor/delete/{id}`   | DELETE | 删除访客         | 软删除       |
|  5   | `/admin/visitor/blacklist`     |  PUT   | **黑名单操作**   | 状态变更     |
|  6   | `/admin/visitor/promote-check` |  GET   | **提升条件检查** | 综合评估     |

**接口5：黑名单操作（核心）**
```java
/**
 * PUT /admin/visitor/blacklist
 * 参数：{visitorId: 1, action: "add"/"remove", reason: "爽约3次"}
 * 
 * 注意：移出黑名单时，要清空爽约次数（给访客改过机会）
 */
@PutMapping("/blacklist")
@Transactional
public R blacklist(@RequestBody BlacklistDTO dto) {
    VisitorEntity visitor = visitorService.getById(dto.getVisitorId());
    Assert.notNull(visitor, "访客不存在");
    
    if ("add".equals(dto.getAction())) {
        // 加入黑名单
        visitor.setStatus(2);
        visitor.setBlacklistEndTime(dto.getEndTime());  // null=永久
        visitor.setBlacklistReason(dto.getReason());
        
        // 记录操作日志
        operateLogService.save("BLACKLIST_ADD", dto);
        
    } else {
        // 移出黑名单：清空爽约次数，给改过机会
        visitor.setStatus(0);
        visitor.setBlacklistEndTime(null);
        visitor.setBlacklistReason(null);
        
        // 【关键】清空爽约记录（或标记为已豁免）
        noShowLogService.clearByVisitorId(dto.getVisitorId());
    }
    
    visitorService.updateById(visitor);
    return R.ok("操作成功");
}
```

### 3.2 申请审批（5接口，核心）

| 序号 | 接口                               | 方法 | 功能         | 技术点     |
| :--: | :--------------------------------- | :--: | :----------- | :--------- |
|  7   | `/admin/application/list`          | GET  | 申请列表     | 状态筛选   |
|  8   | `/admin/application/detail/{id}`   | GET  | 申请详情     | 完整信息   |
|  9   | `/admin/application/approve/{id}`  | PUT  | **审批通过** | 二维码生成 |
|  10  | `/admin/application/reject/{id}`   | PUT  | 审批拒绝     | 原因填写   |
|  11  | `/admin/application/batch-approve` | PUT  | **批量审批** | 事务控制   |

**接口9：审批通过（B的核心难点）**
```java
/**
 * PUT /admin/application/approve/{id}
 * 精简版：专注状态流转和二维码生成，导出统计移给A和C
 */
@PutMapping("/approve/{id}")
@Transactional
public R approve(@PathVariable Long id) {
    ApplicationEntity apply = applicationService.getById(id);
    Assert.notNull(apply, "申请不存在");
    Assert.isTrue(apply.getStatus() == 0, "该申请已处理，当前状态：" + apply.getStatus());
    
    // 1. 生成记录编号（Redis自增，防并发）
    String recordNo = generateRecordNo();
    
    // 2. 生成二维码（含防伪签名）
    String qrContent = recordNo + ":" + apply.getVisitorId() + ":" + apply.getVisitDate();
    String sign = SecureUtil.md5(qrContent + qrSecretKey);
    String qrCode = QRCodeUtil.generateAsBase64(qrContent + ":" + sign, 300, 300);
    
    // 3. 更新申请
    apply.setStatus(1);
    apply.setRecordNo(recordNo);
    apply.setQrCode(qrCode);
    apply.setApproverId(getCurrentUserId());
    apply.setApproveTime(LocalDateTime.now());
    applicationService.updateById(apply);
    
    // 4. 创建记录（用于后续扫码核销）
    createEntryRecord(apply);
    
    // 5. 发送通知（异步，不阻塞）
    smsService.sendApprovedSmsAsync(apply.getVisitorPhone(), recordNo);
    
    return R.ok("审批通过").put("recordNo", recordNo);
}

/**
 * 批量审批（事务控制：全部成功或全部失败）
 */
@PutMapping("/batch-approve")
@Transactional
public R batchApprove(@RequestBody List<Long> ids) {
    List<String> successList = new ArrayList<>();
    List<String> failList = new ArrayList<>();
    
    for (Long id : ids) {
        try {
            // 复用单条审批逻辑
            approve(id);  // 内部调用上面的方法
            successList.add(id.toString());
        } catch (Exception e) {
            failList.add(id + ":" + e.getMessage());
            // 继续处理下一条，不中断
        }
    }
    
    return R.ok()
        .put("successCount", successList.size())
        .put("failCount", failList.size())
        .put("failList", failList);
}
```

### 3.3 入校记录（3接口，精简）

| 序号 | 接口                        | 方法 | 功能     | 技术点   |
| :--: | :-------------------------- | :--: | :------- | :------- |
|  12  | `/admin/record/list`        | GET  | 记录列表 | 分页     |
|  13  | `/admin/record/detail/{id}` | GET  | 记录详情 | 完整信息 |
|  14  | `/admin/record/search`      | GET  | 条件搜索 | 动态SQL  |

> 注意：`/admin/record/today`和`/admin/record/export`移给C，`/admin/record/statistics`移给A

---

## 4. 人员C：统一认证+配置+监控（14接口）

> 特点：系统基础设施，安全责任大，新增实时推送功能

### 4.1 统一认证（4接口）

| 序号 | 接口            | 方法 | 功能          | 技术点   |
| :--: | :-------------- | :--: | :------------ | :------- |
|  1   | `/auth/login`   | POST | **统一登录**  | JWT生成  |
|  2   | `/auth/logout`  | POST | 退出登录      | 黑名单   |
|  3   | `/auth/refresh` | POST | 刷新token     | 续期     |
|  4   | `/auth/verify`  | GET  | **Token验证** | 中间件用 |

### 4.2 管理员管理（5接口）

| 序号 | 接口                         |  方法  | 功能       | 技术点   |
| :--: | :--------------------------- | :----: | :--------- | :------- |
|  5   | `/admin/manager/list`        |  GET   | 管理员列表 | 分页     |
|  6   | `/admin/manager/add`         |  POST  | 添加管理员 | 密码加密 |
|  7   | `/admin/manager/update/{id}` |  PUT   | 修改管理员 | 角色更新 |
|  8   | `/admin/manager/delete/{id}` | DELETE | 删除管理员 | 校验     |
|  9   | `/admin/manager/status/{id}` |  PUT   | 启用/禁用  | 状态变更 |

### 4.3 系统配置（3接口）

| 序号 | 接口                         |  方法   | 功能     | 技术点   |
| :--: | :--------------------------- | :-----: | :------- | :------- |
|  10  | `/admin/config/no-show`      | GET/PUT | 爽约配置 | JSON存储 |
|  11  | `/admin/config/visit-time`   | GET/PUT | 时间配置 | 时段管理 |
|  12  | `/admin/config/special-date` |  POST   | 特殊日期 | 节假日   |

### 4.4 监控与导出（2接口，新增）

| 序号 | 接口                            | 方法 | 功能                    | 技术点    |
| :--: | :------------------------------ | :--: | :---------------------- | :-------- |
|  13  | `/admin/monitor/today-overview` | GET  | **今日入校概览**        | 实时统计  |
|  14  | `/admin/record/export`          | GET  | **记录导出**（从B移来） | EasyExcel |

**接口13：今日入校概览（新增，C的核心难点）**
```java
/**
 * GET /admin/monitor/today-overview
 * 功能：门卫室大屏展示用，实时刷新今日数据
 * 
 * 返回：
 * {
 *   "totalAppointment": 100,  // 今日预约总人数
 *   "entered": 45,            // 已入校
 *   "notEntered": 55,         // 未入校（含爽约）
 *   "currentPeriod": {         // 当前时段
 *     "timeSlot": "09:00-12:00",
 *     "count": 30,
 *     "entered": 15
 *   },
 *   "gateStats": [            // 各校门统计
 *     {"gate": "南门", "entered": 20, "leaved": 15},
 *     {"gate": "北门", "entered": 25, "leaved": 10}
 *   ]
 * }
 */
@GetMapping("/today-overview")
public R todayOverview() {
    LocalDate today = LocalDate.now();
    
    // 今日总预约
    int totalAppointment = applicationService.count(
        new QueryWrapper<ApplicationEntity>()
            .eq("visit_date", today)
            .in("status", 1, 4, 5)  // 已通过、已爽约、已完成
    );
    
    // 已入校（有entry_time的记录）
    int entered = entryRecordService.count(
        new QueryWrapper<EntryRecordEntity>()
            .eq("visit_date", today)
            .isNotNull("entry_time")
    );
    
    // 当前时段（根据当前时间判断）
    String currentSlot = getCurrentTimeSlot();  // 09:00-12:00等
    Map<String, Object> currentPeriod = getPeriodStats(today, currentSlot);
    
    // 校门统计（需扫码设备上报数据）
    List<Map<String, Object>> gateStats = entryRecordService.getGateStats(today);
    
    return R.ok()
        .put("totalAppointment", totalAppointment)
        .put("entered", entered)
        .put("notEntered", totalAppointment - entered)
        .put("currentPeriod", currentPeriod)
        .put("gateStats", gateStats);
}

/**
 * 实时推送（SSE，比WebSocket简单）
 * 场景：门卫室大屏，有人扫码入校时自动刷新
 */
@GetMapping(value = "/realtime", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter realtime() {
    SseEmitter emitter = new SseEmitter(0L);
    
    // 订阅Redis频道，有新扫码事件时推送
    redisTemplate.convertAndSend("entry:subscribe", "new");
    
    redisMessageListenerContainer.addMessageListener((message, pattern) -> {
        String payload = new String(message.getBody());
        try {
            emitter.send(payload);
        } catch (IOException e) {
            emitter.complete();
        }
    }, new PatternTopic("entry:channel"));
    
    return emitter;
}
```

---

## 5. 数据库表结构（按人员划分）

### 5.1 人员A负责表

```sql
-- 访客用户表
CREATE TABLE `ers_visitor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访客ID',
  `phone` varchar(20) NOT NULL COMMENT '手机号（登录账号）',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `id_card` varchar(18) NOT NULL COMMENT '身份证号',
  `gender` tinyint DEFAULT 1 COMMENT '性别 0-女 1-男',
  `avatar` varchar(200) DEFAULT NULL COMMENT '头像URL',
  `status` tinyint DEFAULT 0 COMMENT '状态 0-正常 1-禁用 2-黑名单',
  `blacklist_end_time` datetime DEFAULT NULL COMMENT '黑名单结束时间',
  `blacklist_reason` varchar(200) DEFAULT NULL COMMENT '拉黑原因',
  `promoted_to_admin` tinyint DEFAULT 0 COMMENT '是否已提升 0-否 1-是',
  `admin_id` bigint DEFAULT NULL COMMENT '关联管理员ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='访客用户表';

-- 入校申请表（A创建，B审批）
CREATE TABLE `ers_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `visitor_id` bigint NOT NULL COMMENT '访客ID',
  `visitor_name` varchar(50) NOT NULL COMMENT '冗余：姓名',
  `visitor_phone` varchar(20) NOT NULL COMMENT '冗余：手机号',
  `visitor_id_card` varchar(18) NOT NULL COMMENT '冗余：身份证',
  `visit_date` date NOT NULL COMMENT '预约日期',
  `time_slot` varchar(20) NOT NULL COMMENT '时间段',
  `visit_purpose` varchar(500) NOT NULL COMMENT '入校事由',
  `visit_target` varchar(100) DEFAULT NULL COMMENT '拜访对象',
  `status` tinyint DEFAULT 0 COMMENT '0-待审批 1-已通过 2-已拒绝 3-已取消 4-已爽约 5-已完成',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `record_no` varchar(50) DEFAULT NULL COMMENT '记录编号（审批后生成）',
  `qr_code` text COMMENT '二维码Base64',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `entry_time` datetime DEFAULT NULL COMMENT '实际进校时间',
  `leave_time` datetime DEFAULT NULL COMMENT '实际离校时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_no` (`record_no`),
  KEY `idx_visitor_id` (`visitor_id`),
  KEY `idx_visit_date` (`visit_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB COMMENT='入校申请表';
```

### 5.2 人员B负责表

```sql
-- 入校记录表（B创建，扫码时更新）
CREATE TABLE `ers_entry_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `record_no` varchar(50) NOT NULL COMMENT '记录编号',
  `application_id` bigint NOT NULL COMMENT '关联申请ID',
  `visitor_id` bigint NOT NULL COMMENT '访客ID',
  `visitor_name` varchar(50) NOT NULL COMMENT '冗余：姓名',
  `visitor_phone` varchar(20) NOT NULL COMMENT '冗余：手机号',
  `visitor_id_card` varchar(18) NOT NULL COMMENT '冗余：身份证',
  `visit_date` date NOT NULL COMMENT '预约日期',
  `time_slot` varchar(20) NOT NULL COMMENT '时间段',
  `entry_time` datetime DEFAULT NULL COMMENT '实际进校时间',
  `leave_time` datetime DEFAULT NULL COMMENT '实际离校时间',
  `status` tinyint DEFAULT 0 COMMENT '0-未入校 1-已入校 2-已离校 3-爽约',
  `entry_gate` varchar(50) DEFAULT NULL COMMENT '入校校门',
  `leave_gate` varchar(50) DEFAULT NULL COMMENT '离校校门',
  `entry_operator` bigint DEFAULT NULL COMMENT '入校操作员',
  `leave_operator` bigint DEFAULT NULL COMMENT '离校操作员',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_record_no` (`record_no`),
  KEY `idx_visit_date` (`visit_date`)
) ENGINE=InnoDB COMMENT='入校记录表';

-- 黑名单操作日志（B操作，记录审计）
CREATE TABLE `ers_blacklist_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `visitor_id` bigint NOT NULL COMMENT '访客ID',
  `action` varchar(20) NOT NULL COMMENT '操作 add/remove',
  `reason` varchar(200) DEFAULT NULL COMMENT '原因',
  `operator_id` bigint NOT NULL COMMENT '操作人',
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB COMMENT='黑名单操作日志';
```

### 5.3 人员C负责表

```sql
-- 管理员表
CREATE TABLE `ers_manager` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `role_type` tinyint DEFAULT 1 COMMENT '1-普通管理员 2-超级管理员',
  `status` tinyint DEFAULT 1 COMMENT '0-禁用 1-启用',
  `source_visitor_id` bigint DEFAULT NULL COMMENT '来源访客ID（被提升时填充）',
  `promote_time` datetime DEFAULT NULL COMMENT '提升时间',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB COMMENT='管理员表';

-- 角色表
CREATE TABLE `ers_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `permissions` json COMMENT '权限列表',
  `remark` varchar(200) DEFAULT NULL,
  `sort` int DEFAULT 0,
  `status` tinyint DEFAULT 1,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`role_code`)
) ENGINE=InnoDB COMMENT='角色表';

-- 管理员-角色关联
CREATE TABLE `ers_manager_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `manager_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_manager_role` (`manager_id`, `role_id`)
) ENGINE=InnoDB COMMENT='管理员角色关联';

-- 系统配置表
CREATE TABLE `ers_config` (
  `config_key` varchar(100) NOT NULL,
  `config_value` text COMMENT 'JSON格式',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB COMMENT='系统配置表';

-- 爽约记录表（C定时任务写入）
CREATE TABLE `ers_no_show_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `visitor_id` bigint NOT NULL COMMENT '访客ID',
  `application_id` bigint NOT NULL COMMENT '申请ID',
  `no_show_date` date NOT NULL COMMENT '爽约日期',
  `is_cleared` tinyint DEFAULT 0 COMMENT '是否被清空 0-否 1-是（移出黑名单时）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_visitor_id` (`visitor_id`)
) ENGINE=InnoDB COMMENT='爽约记录表';

-- Token黑名单（Redis也可，但表更持久）
CREATE TABLE `ers_token_blacklist` (
  `token` varchar(500) NOT NULL COMMENT 'Token值',
  `expire_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token`(255)),
  KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB COMMENT='Token黑名单';

-- 初始化配置
INSERT INTO `ers_config` VALUES 
('no_show_rule', '{"lateMinutes":30,"noShowLimit":3,"blacklistDays":7}', NOW()),
('visit_time_rule', '{"timeSlots":[{"startTime":"08:00","endTime":"12:00","maxCount":100,"enabled":true},{"startTime":"14:00","endTime":"18:00","maxCount":100,"enabled":true}]}', NOW()),
('special_dates', '[]', NOW());
```

---

## 6. 接口汇总表（最终均衡版）

| 人员        | 序号 | 接口                               |  方法   | 功能           | 难度 |
| :---------- | :--: | :--------------------------------- | :-----: | :------------- | :--: |
| **A（14）** |  1   | `/api/user/register`               |  POST   | 注册           |  ⭐   |
|             |  2   | `/api/user/login`                  |  POST   | 登录           |  ⭐⭐  |
|             |  3   | `/api/user/info`                   |   GET   | 个人信息       |  ⭐   |
|             |  4   | `/api/user/update`                 |   PUT   | 修改信息       |  ⭐   |
|             |  5   | `/api/user/password`               |   PUT   | 修改密码       |  ⭐⭐  |
|             |  6   | `/api/user/dashboard`              |   GET   | **数据看板**   | ⭐⭐⭐⭐ |
|             |  7   | `/api/user/calendar`               |   GET   | **入校日历**   | ⭐⭐⭐⭐ |
|             |  8   | `/api/application/submit`          |  POST   | 提交申请       |  ⭐⭐  |
|             |  9   | `/api/application/list`            |   GET   | 申请列表       |  ⭐⭐  |
|             |  10  | `/api/application/detail/{id}`     |   GET   | 申请详情       |  ⭐   |
|             |  11  | `/api/application/timeline/{id}`   |   GET   | **进度时间轴** | ⭐⭐⭐  |
|             |  12  | `/api/application/cancel/{id}`     |   PUT   | 取消申请       |  ⭐⭐  |
|             |  13  | `/api/record/my`                   |   GET   | 我的记录       |  ⭐   |
|             |  14  | `/admin/visitor/export`            |   GET   | **访客导出**   | ⭐⭐⭐  |
| **B（14）** |  1   | `/admin/visitor/list`              |   GET   | 访客列表       |  ⭐⭐  |
|             |  2   | `/admin/visitor/detail/{id}`       |   GET   | 访客详情       |  ⭐⭐  |
|             |  3   | `/admin/visitor/update/{id}`       |   PUT   | 修改访客       |  ⭐⭐  |
|             |  4   | `/admin/visitor/delete/{id}`       | DELETE  | 删除访客       |  ⭐⭐  |
|             |  5   | `/admin/visitor/blacklist`         |   PUT   | **黑名单**     | ⭐⭐⭐  |
|             |  6   | `/admin/visitor/promote-check`     |   GET   | **提升检查**   | ⭐⭐⭐  |
|             |  7   | `/admin/application/list`          |   GET   | 申请列表       |  ⭐⭐  |
|             |  8   | `/admin/application/detail/{id}`   |   GET   | 申请详情       |  ⭐   |
|             |  9   | `/admin/application/approve/{id}`  |   PUT   | **审批通过**   | ⭐⭐⭐⭐ |
|             |  10  | `/admin/application/reject/{id}`   |   PUT   | 审批拒绝       | ⭐⭐⭐  |
|             |  11  | `/admin/application/batch-approve` |   PUT   | **批量审批**   | ⭐⭐⭐⭐ |
|             |  12  | `/admin/record/list`               |   GET   | 记录列表       |  ⭐⭐  |
|             |  13  | `/admin/record/detail/{id}`        |   GET   | 记录详情       |  ⭐   |
|             |  14  | `/admin/record/search`             |   GET   | 条件搜索       | ⭐⭐⭐  |
| **C（14）** |  1   | `/auth/login`                      |  POST   | **统一登录**   | ⭐⭐⭐⭐ |
|             |  2   | `/auth/logout`                     |  POST   | 退出登录       |  ⭐⭐  |
|             |  3   | `/auth/refresh`                    |  POST   | 刷新token      | ⭐⭐⭐  |
|             |  4   | `/auth/verify`                     |   GET   | **Token验证**  | ⭐⭐⭐  |
|             |  5   | `/admin/manager/list`              |   GET   | 管理员列表     |  ⭐   |
|             |  6   | `/admin/manager/add`               |  POST   | 添加管理员     |  ⭐⭐  |
|             |  7   | `/admin/manager/update/{id}`       |   PUT   | 修改管理员     |  ⭐⭐  |
|             |  8   | `/admin/manager/delete/{id}`       | DELETE  | 删除管理员     |  ⭐⭐  |
|             |  9   | `/admin/manager/status/{id}`       |   PUT   | 启用/禁用      |  ⭐   |
|             |  10  | `/admin/config/no-show`            | GET/PUT | 爽约配置       | ⭐⭐⭐  |
|             |  11  | `/admin/config/visit-time`         | GET/PUT | 时间配置       | ⭐⭐⭐  |
|             |  12  | `/admin/config/special-date`       |  POST   | 特殊日期       |  ⭐⭐  |
|             |  13  | `/admin/monitor/today-overview`    |   GET   | **今日概览**   | ⭐⭐⭐⭐ |
|             |  14  | `/admin/record/export`             |   GET   | **记录导出**   | ⭐⭐⭐  |

---

## 7. 三人难度对比（均衡后）

| 维度         |         A         |         B         |         C         |     均衡性     |
| :----------- | :---------------: | :---------------: | :---------------: | :------------: |
| 接口数量     |        14         |        14         |        14         |   ✅ 完全相等   |
| 4星难点      | 2个（看板、日历） | 2个（审批、批量） | 2个（登录、概览） |     ✅ 相等     |
| 3星难点      |        2个        |        3个        |        3个        |      合理      |
| 代码行数     |       ~1800       |       ~1800       |       ~1800       |     ✅ 相当     |
| 业务复杂度   |     数据处理      |     状态流转      |     系统安全      |    各有侧重    |
| 联调依赖     |        中         |        高         |        中         |      合理      |
| **综合难度** |       ⭐⭐⭐⭐        |       ⭐⭐⭐⭐        |       ⭐⭐⭐⭐        | ✅ **完全均衡** |

---

## 8. 协作边界说明

| 协作点     |    A     |  B   |       C        | 说明             |
| :--------- | :------: | :--: | :------------: | :--------------- |
| 申请表结构 |   设计   | 使用 |       -        | A定字段，B只读   |
| 访客状态   |   维护   | 查询 | 修改（黑名单） | 状态值约定       |
| 时间配置   |   查询   |  -   |      维护      | A读C的配置       |
| 记录导出   | 访客导出 |  -   |    记录导出    | 各导各的数据     |
| 今日数据   | 个人视角 |  -   |    全局视角    | C给B提供实时数据 |

---

**文档完成，三人工作量完全均衡，可直接开发！**