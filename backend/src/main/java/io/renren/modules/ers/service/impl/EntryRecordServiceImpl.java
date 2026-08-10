package io.renren.modules.ers.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.modules.ers.dao.EntryRecordDao;
import io.renren.modules.ers.entity.EntryRecordEntity;
import io.renren.modules.ers.entity.ErsTimeSlotEntity;
import io.renren.modules.ers.service.EntryRecordService;
import io.renren.modules.ers.service.ErsTimeSlotService;
import io.renren.modules.ers.vo.EntryRecordExcelVO;
import io.renren.modules.ers.vo.ApplicationExcelVO;
import io.renren.modules.application.entity.ApplicationEntity;
import io.renren.modules.application.service.ApplicationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("entryRecordService")
public class EntryRecordServiceImpl extends ServiceImpl<EntryRecordDao, EntryRecordEntity> implements EntryRecordService {

    @Autowired
    private ErsTimeSlotService ersTimeSlotService;
    @Autowired
    private ApplicationService applicationService;

    @Override
    public Map<String, Object> getTodayOverview() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        LocalTime currentTime = now.toLocalTime();

        QueryWrapper<EntryRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("entry_date", today);

        List<EntryRecordEntity> todayRecords = this.list(queryWrapper);

        int totalAppointment = 0;
        int entered = 0;
        int notEntered = 0;
        int currentPeriodCount = 0;
        int currentPeriodEntered = 0;

        List<Map<String, Object>> gateStats = new ArrayList<>();
        Map<String, Integer> gateEnteredMap = new HashMap<>();
        Map<String, Integer> gateLeavedMap = new HashMap<>();

        for (EntryRecordEntity record : todayRecords) {
            if (record.getRecordStatus() != null) {
                if (record.getRecordStatus() == 1 || record.getRecordStatus() == 4 || record.getRecordStatus() == 5) {
                    totalAppointment++;
                }

                if (record.getActualEntryTime() != null) {
                    entered++;
                } else {
                    notEntered++;
                }

                if (record.getEntryStartTime() != null && record.getEntryEndTime() != null) {
                    LocalDateTime startDateTime = convertToLocalDateTime(record.getEntryStartTime());
                    LocalDateTime endDateTime = convertToLocalDateTime(record.getEntryEndTime());
                    LocalTime startTime = startDateTime.toLocalTime();
                    LocalTime endTime = endDateTime.toLocalTime();

                    if (!currentTime.isBefore(startTime) && !currentTime.isAfter(endTime)) {
                        currentPeriodCount++;
                        if (record.getActualEntryTime() != null) {
                            currentPeriodEntered++;
                        }
                    }
                }

                if (StringUtils.isNotBlank(record.getVerifyGate())) {
                    gateEnteredMap.merge(record.getVerifyGate(), 1, Integer::sum);
                    if (record.getActualEntryTime() != null) {
                        gateLeavedMap.merge(record.getVerifyGate(), 1, Integer::sum);
                    }
                }
            }
        }

        for (String gate : gateEnteredMap.keySet()) {
            Map<String, Object> gateStat = new HashMap<>();
            gateStat.put("gate", gate);
            gateStat.put("entered", gateEnteredMap.get(gate));
            gateStat.put("leaved", gateLeavedMap.getOrDefault(gate, 0));
            gateStats.add(gateStat);
        }

        String currentTimeSlot = getCurrentTimeSlot(currentTime);

        Map<String, Object> currentPeriod = new HashMap<>();
        currentPeriod.put("timeSlot", currentTimeSlot);
        currentPeriod.put("count", currentPeriodCount);
        currentPeriod.put("entered", currentPeriodEntered);

        QueryWrapper<ApplicationEntity> appQueryWrapper = new QueryWrapper<>();
        appQueryWrapper.eq("entry_date", today.toString()).eq("status", 1);
        List<ApplicationEntity> todayApplications = applicationService.list(appQueryWrapper);

        List<Map<String, Object>> todayApplicationList = new ArrayList<>();
        for (ApplicationEntity app : todayApplications) {
            Map<String, Object> appMap = new HashMap<>();
            appMap.put("id", app.getId());
            appMap.put("applicationNo", app.getApplicationNo());
            appMap.put("visitorName", app.getVisitorName());
            appMap.put("phone", app.getPhone());
            appMap.put("entryDate", app.getEntryDate());
            appMap.put("entryStartTime", app.getEntryStartTime());
            appMap.put("entryEndTime", app.getEntryEndTime());
            appMap.put("reason", app.getReason());
            todayApplicationList.add(appMap);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalAppointment", totalAppointment);
        result.put("entered", entered);
        result.put("notEntered", notEntered);
        result.put("currentPeriod", currentPeriod);
        result.put("gateStats", gateStats);
        result.put("todayApplications", todayApplicationList);
        result.put("message", todayApplicationList.isEmpty() ? "今日没有入校申请" : null);

        return result;
    }

    private String getCurrentTimeSlot(LocalTime currentTime) {
        List<ErsTimeSlotEntity> slots = ersTimeSlotService.list(
                new QueryWrapper<ErsTimeSlotEntity>().eq("status", 1).orderByAsc("sort")
        );

        for (ErsTimeSlotEntity slot : slots) {
            if (slot.getStartTime() == null || slot.getEndTime() == null) {
                continue;
            }
            LocalTime start = LocalTime.parse(slot.getStartTime().substring(0, 5));
            LocalTime end = LocalTime.parse(slot.getEndTime().substring(0, 5));

            if (!currentTime.isBefore(start) && !currentTime.isAfter(end)) {
                return slot.getStartTime().substring(0, 5) + "-" + slot.getEndTime().substring(0, 5);
            }
        }

        if (!currentTime.isBefore(LocalTime.of(8, 0)) && !currentTime.isAfter(LocalTime.of(12, 0))) {
            return "08:00-12:00";
        } else if (!currentTime.isBefore(LocalTime.of(14, 0)) && !currentTime.isAfter(LocalTime.of(18, 0))) {
            return "14:00-18:00";
        } else if (!currentTime.isBefore(LocalTime.of(18, 0))) {
            return "18:00-20:00";
        }
        return "无";
    }

    private LocalDateTime convertToLocalDateTime(java.util.Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    @Override
    public List<EntryRecordEntity> queryPage(Map<String, Object> params) {
        String recordNo = (String) params.get("recordNo");
        String visitorName = (String) params.get("visitorName");
        String phone = (String) params.get("phone");
        String startDate = (String) params.get("startDate");
        String endDate = (String) params.get("endDate");
        Integer recordStatus = params.get("recordStatus") != null ? Integer.valueOf(params.get("recordStatus").toString()) : null;

        QueryWrapper<EntryRecordEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StringUtils.isNotBlank(recordNo) && !isInvalidDate(recordNo), "record_no", recordNo)
                .like(StringUtils.isNotBlank(visitorName), "visitor_name", visitorName)
                .like(StringUtils.isNotBlank(phone), "phone", phone)
                .ge(StringUtils.isNotBlank(startDate) && isValidDate(startDate), "entry_date", startDate)
                .le(StringUtils.isNotBlank(endDate) && isValidDate(endDate), "entry_date", endDate)
                .eq(recordStatus != null, "record_status", recordStatus)
                .orderByDesc("create_time");

        return this.list(queryWrapper);
    }

    private boolean isValidDate(String str) {
        if (str == null || str.length() != 10) return false;
        return str.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    private boolean isInvalidDate(String str) {
        return "开始日期".equals(str) || "结束日期".equals(str) || "请选择".equals(str);
    }

    @Override
    public void exportRecords(Map<String, Object> params, HttpServletResponse response) throws IOException {
        List<EntryRecordEntity> list = queryPage(params);

        List<EntryRecordExcelVO> excelList = list.stream().map(record -> {
            EntryRecordExcelVO vo = new EntryRecordExcelVO();
            vo.setRecordNo(record.getRecordNo());
            vo.setVisitorName(record.getVisitorName());
            vo.setPhone(record.getPhone());
            vo.setEntryDate(record.getEntryDate());
            vo.setEntryStartTime(record.getEntryStartTime());
            vo.setEntryEndTime(record.getEntryEndTime());
            vo.setActualEntryTime(record.getActualEntryTime());
            vo.setVerifyStatus(record.getVerifyStatus() != null && record.getVerifyStatus() == 1 ? "已核销" : "未核销");
            vo.setVerifyGate(record.getVerifyGate());
            vo.setRecordStatus(convertRecordStatus(record.getRecordStatus()));
            vo.setRemark(record.getRemark());
            return vo;
        }).collect(Collectors.toList());

        String fileName = URLEncoder.encode("入校记录导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), EntryRecordExcelVO.class)
                .sheet("入校记录")
                .doWrite(excelList);
    }

    private String convertRecordStatus(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待入校";
            case 1: return "已入校";
            case 2: return "已过期";
            case 3: return "已爽约";
            case 4: return "已完成";
            default: return "未知";
        }
    }

    @Override
    public void exportApplications(Map<String, Object> params, HttpServletResponse response) throws IOException {
        QueryWrapper<ApplicationEntity> queryWrapper = new QueryWrapper<>();

        String startDate = null;
        if (params.get("startDate") != null) {
            String val = params.get("startDate").toString();
            if (val.matches("\\d{4}-\\d{2}-\\d{2}")) {
                startDate = val;
            }
        }
        String endDate = null;
        if (params.get("endDate") != null) {
            String val = params.get("endDate").toString();
            if (val.matches("\\d{4}-\\d{2}-\\d{2}")) {
                endDate = val;
            }
        }
        Integer status = null;
        if (params.get("status") != null) {
            String statusStr = params.get("status").toString();
            if (statusStr.matches("\\d+")) {
                status = Integer.valueOf(statusStr);
            }
        }

        if (startDate != null && !startDate.isEmpty()) {
            queryWrapper.ge("entry_date", startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            queryWrapper.le("entry_date", endDate);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }

        queryWrapper.orderByDesc("create_time");
        List<ApplicationEntity> list = applicationService.list(queryWrapper);

        List<ApplicationExcelVO> excelList = list.stream().map(app -> {
            ApplicationExcelVO vo = new ApplicationExcelVO();
            vo.setApplicationNo(app.getApplicationNo());
            vo.setVisitorName(app.getVisitorName());
            vo.setPhone(app.getPhone());
            vo.setEntryDate(app.getEntryDate());
            vo.setEntryStartTime(app.getEntryStartTime());
            vo.setEntryEndTime(app.getEntryEndTime());
            vo.setReason(app.getReason());
            vo.setStatusText(convertApplicationStatus(app.getStatus()));
            vo.setApprovalTime(app.getApprovalTime());
            vo.setApprovalRemark(app.getApprovalRemark());
            return vo;
        }).collect(Collectors.toList());

        String fileName = URLEncoder.encode("入校申请导出", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ApplicationExcelVO.class)
                .sheet("入校申请")
                .doWrite(excelList);
    }

    private String convertApplicationStatus(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待审批";
            case 1: return "已通过";
            case 2: return "已拒绝";
            case 3: return "已取消";
            case 4: return "已爽约";
            case 5: return "已完成";
            default: return "未知";
        }
    }
}