package io.renren.modules.ers.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.R;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.ers.service.EntryRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/monitor")
public class MonitorController {
    @Autowired
    private EntryRecordService entryRecordService;
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
            String userType = claims.get("userType", String.class);
            if(!"ADMIN".equals(userType)){
                return R.error("无权限操作，需要管理员权限");
            }
            return null;
        } catch (Exception e) {
            return R.error("token无效，请重新登录");
        }
    }

    @SysLog("查看今日概览")
    @GetMapping("/today-overview")
    public R todayOverview(@RequestHeader("token") String token) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        Map<String, Object> result = entryRecordService.getTodayOverview();
        return R.ok(result);
    }
}
