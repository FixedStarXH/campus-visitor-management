package io.renren.modules.visitor.controller;

import io.jsonwebtoken.Claims;
import io.renren.common.utils.R;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.visitor.service.VisitorExportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/visitor")
@Api("访客导出接口")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class VisitorExportController {

    @Autowired
    private VisitorExportService visitorExportService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/export")
    @ApiOperation("导出个人入校申请记录")
    public void export(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. 获取并校验 token
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return;
        }

        // 2. 检查黑名单
        Boolean isBlacklisted = stringRedisTemplate.hasKey("blacklist:" + token);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            response.setStatus(401);
            return;
        }

        // 3. 解析 token 获取用户 ID
        Claims claims = jwtUtils.getClaimByToken(token);
        if (claims == null || jwtUtils.isTokenExpired(claims.getExpiration())) {
            response.setStatus(401);
            return;
        }
        Long userId = Long.parseLong(claims.getSubject());

        // 4. 导出个人申请记录
        visitorExportService.exportUserExcel(userId, response);
    }
}