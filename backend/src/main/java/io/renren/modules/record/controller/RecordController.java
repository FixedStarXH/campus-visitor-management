package io.renren.modules.record.controller;

import io.renren.common.utils.R;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.record.form.RecordQueryForm;
import io.renren.modules.record.service.RecordService;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/record")
@Api("入校记录接口")
public class RecordController {

    @Autowired
    private RecordService recordService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("/my")
    @ApiOperation("我的入校记录查询")
    public R myRecord(RecordQueryForm form, HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            return R.error("token不能为空");
        }

        Boolean isBlacklisted = stringRedisTemplate.hasKey("blacklist:" + token);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            return R.error("token已失效，请重新登录");
        }

        Claims claims = jwtUtils.getClaimByToken(token);
        if (claims == null || jwtUtils.isTokenExpired(claims.getExpiration())) {
            return R.error("token无效或已过期");
        }
        Long visitorId = Long.parseLong(claims.getSubject());

        Map<String, Object> data = recordService.listPage(form, visitorId);
        return R.ok(data);
    }
}