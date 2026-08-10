package io.renren.modules.application.controller;

import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.application.entity.ApplicationEntity;
import io.renren.modules.application.form.ApplicationForm;
import io.renren.modules.application.form.ApplicationQueryForm;
import io.renren.modules.application.service.ApplicationService;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/application")
@Api("入校申请接口")
public class AppApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/submit")
    @ApiOperation("提交入校申请")
    public R submit(HttpServletRequest request, @RequestBody ApplicationForm form) {
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

        ValidatorUtils.validateEntity(form);

        Long applicationId = applicationService.submit(form, visitorId);

        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", applicationId);
        return R.ok(data);
    }

    @GetMapping("/list")
    @ApiOperation("申请个人列表查询")
    public R list(ApplicationQueryForm form, HttpServletRequest request) {
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

        Map<String, Object> data = applicationService.listPage(form, visitorId);
        return R.ok(data);
    }

    @GetMapping("/detail/{id}")
    @ApiOperation("申请详情查询")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request) {
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

        ApplicationEntity application = applicationService.getDetailById(id, visitorId);
        if (application == null) {
            return R.error("申请不存在或无权访问");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("detail", application);
        return R.ok(data);
    }

    @PostMapping("/cancel/{id}")
    @ApiOperation("取消入校申请")
    public R cancel(@PathVariable("id") Long id, HttpServletRequest request) {
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

        boolean success = applicationService.cancel(id, visitorId);
        if (!success) {
            return R.error("取消失败，仅待审批状态的申请可取消");
        }

        return R.ok("取消成功");
    }
}