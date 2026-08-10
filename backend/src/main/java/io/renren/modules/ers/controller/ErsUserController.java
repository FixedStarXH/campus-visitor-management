package io.renren.modules.ers.controller;

import io.renren.common.utils.R;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.ers.entity.ErsEntryApplicationEntity;
import io.renren.modules.ers.service.ErsEntryApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 访客端接口 Controller
 */
@RestController
@RequestMapping("/api/ers/user")
public class ErsUserController {

    @Autowired
    private ErsEntryApplicationService applicationService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private R verifyToken(String token) {
        if(token == null || token.isEmpty()){
            return R.error("token不能为空");
        }
        try {
            if(stringRedisTemplate.hasKey("blacklist:" + token)){
                return R.error("token已失效，请重新登录");
            }
            io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
            if(claims == null || jwtUtils.isTokenExpired(claims.getExpiration())){
                return R.error("token失效，请重新登录");
            }
            return null;
        } catch (Exception e) {
            return R.error("token无效，请重新登录");
        }
    }

    /**
     * 个人入校日历
     * GET /api/ers/user/calendar?month=2024-04
     * 功能：按月展示哪些日期有入校安排（类似签到日历）
     */
    @GetMapping("/calendar")
    public R calendar(@RequestHeader("token") String token, @RequestParam(required = false) String month) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        try {
            io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
            Long visitorId = Long.parseLong(claims.getSubject());

            if (month == null || month.isEmpty()) {
                month = LocalDate.now().toString().substring(0, 7);
            }

            LocalDate start = LocalDate.parse(month + "-01");
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

            List<ErsEntryApplicationEntity> list = applicationService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ErsEntryApplicationEntity>()
                    .eq("visitor_id", visitorId)
                    .between("entry_date", start.toString(), end.toString())
            );

            Map<LocalDate, List<CalendarVO>> calendarMap = list.stream()
                .collect(Collectors.groupingBy(
                    app -> LocalDate.parse(app.getEntryDate()),
                    Collectors.mapping(app -> {
                        CalendarVO vo = new CalendarVO();
                        vo.setId(app.getApplicationId());
                        vo.setTimeSlot(app.getTimeSlot());
                        vo.setStatus(app.getStatus());
                        vo.setStatusText(convertStatus(app.getStatus()));
                        vo.setHasQrCode(app.getStatus() == 1);
                        return vo;
                    }, Collectors.toList())
                ));

            List<Map<String, Object>> result = new ArrayList<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                Map<String, Object> day = new HashMap<>();
                day.put("date", date.toString());
                day.put("dayOfWeek", date.getDayOfWeek().getValue());
                day.put("items", calendarMap.getOrDefault(date, new ArrayList<>()));
                result.add(day);
            }

            return R.ok().put("calendar", result);
        } catch (Exception e) {
            e.printStackTrace();
            return R.error(500, "服务器内部错误：" + e.getMessage());
        }
    }

    private String convertStatus(Integer status) {
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

    private static class CalendarVO {
        private Long id;
        private String timeSlot;
        private Integer status;
        private String statusText;
        private boolean hasQrCode;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTimeSlot() { return timeSlot; }
        public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStatusText() { return statusText; }
        public void setStatusText(String statusText) { this.statusText = statusText; }
        public boolean isHasQrCode() { return hasQrCode; }
        public void setHasQrCode(boolean hasQrCode) { this.hasQrCode = hasQrCode; }
    }
}