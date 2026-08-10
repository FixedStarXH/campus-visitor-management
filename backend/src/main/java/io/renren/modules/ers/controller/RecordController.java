package io.renren.modules.ers.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.R;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.ers.service.EntryRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController("ersRecordController")
@RequestMapping("/admin/record")
public class RecordController {
    private static final Logger logger = LoggerFactory.getLogger(RecordController.class);

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

    @SysLog("导出入校记录")
    @GetMapping("/export")
    public void export(@RequestHeader("token") String token, @RequestParam Map<String, Object> params, HttpServletResponse response) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) {
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"" + verifyResult.get("msg") + "\"}");
            } catch (IOException ex) {
                logger.error("写入错误响应失败", ex);
            }
            return;
        }

        try {
            entryRecordService.exportRecords(params, response);
        } catch (Exception e) {
            logger.error("导出记录失败", e);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":500,\"msg\":\"导出失败：" + e.getMessage() + "\"}");
            } catch (IOException ex) {
                logger.error("写入错误响应失败", ex);
            }
        }
    }

    @SysLog("导出入校申请")
    @GetMapping("/export-application")
    public void exportApplication(@RequestHeader("token") String token, @RequestParam Map<String, Object> params, HttpServletResponse response) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) {
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"" + verifyResult.get("msg") + "\"}");
            } catch (IOException ex) {
                logger.error("写入错误响应失败", ex);
            }
            return;
        }

        try {
            entryRecordService.exportApplications(params, response);
        } catch (Exception e) {
            logger.error("导出申请失败", e);
            response.setContentType("application/json;charset=UTF-8");
            try {
                response.getWriter().write("{\"code\":500,\"msg\":\"导出失败：" + e.getMessage() + "\"}");
            } catch (IOException ex) {
                logger.error("写入错误响应失败", ex);
            }
        }
    }
}
