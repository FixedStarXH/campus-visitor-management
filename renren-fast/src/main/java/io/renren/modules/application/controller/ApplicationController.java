package io.renren.modules.application.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.application.entity.ApplicationEntity;
import io.renren.modules.application.form.ApplicationForm;
import io.renren.modules.application.form.ApplicationQueryForm;
import io.renren.modules.application.service.ApplicationService;
import io.renren.modules.sys.controller.AbstractController;
import io.jsonwebtoken.Claims;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 入校申请控制器
 * 包含管理员后台接口和用户API接口
 */
@RestController
@RequestMapping("/admin/application")
@Api(tags = "入校审批管理")
public class ApplicationController extends AbstractController {

    @Autowired
    private ApplicationService applicationService;

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

    // ==================== 管理员后台接口 ====================

    /**
     * 1. 入校申请列表
     */
    @GetMapping("/list")
    @ApiOperation("入校申请列表")
    public R list(@RequestHeader("token") String token, @RequestParam Map<String, Object> params) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        PageUtils page = applicationService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 2. 入校申请详情
     */
    @GetMapping("/detail/{id}")
    @ApiOperation("入校申请详情")
    public R detail(@RequestHeader("token") String token, @PathVariable("id") Long id) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        ApplicationEntity application = applicationService.getDetail(id);
        return R.ok().put("application", application);
    }

    /**
     * 3. 入校申请审批通过
     */
    @GetMapping("/approve/{id}")
    @SysLog("入校申请审批通过")
    @ApiOperation("入校申请审批通过")
    public R approve(@RequestHeader("token") String token, @PathVariable("id") Long id) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        applicationService.approve(id, userId);
        return R.ok("审批通过");
    }

    /**
     * 4. 入校申请审批拒绝
     */
    @PutMapping("/reject/{id}")
    @SysLog("入校申请审批拒绝")
    @ApiOperation("入校申请审批拒绝")
    public R reject(@RequestHeader("token") String token, @PathVariable("id") Long id, @RequestBody Map<String, String> params) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        String reason = params.get("reason");
        io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        applicationService.reject(id, reason, userId);
        return R.ok("已拒绝");
    }

    /**
     * 5. 批量审批通过
     */
    @PutMapping("/batch-approve")
    @SysLog("批量审批通过")
    @ApiOperation("批量审批通过")
    public R batchApprove(@RequestHeader("token") String token, @RequestBody Map<String, Object> params) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        List<Long> ids = (List<Long>) params.get("ids");
        String remark = (String) params.get("remark");
        io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        applicationService.batchApprove(ids, remark, userId);
        return R.ok("批量审批通过");
    }

    /**
     * 6. 单个删除入校申请
     */
    @DeleteMapping("/delete/{id}")
    @SysLog("单个删除入校申请")
    @ApiOperation("单个删除入校申请")
    public R delete(@RequestHeader("token") String token, @PathVariable("id") Long id) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        applicationService.delete(id);
        return R.ok("删除成功");
    }

    /**
     * 7. 批量删除入校申请
     */
    @DeleteMapping("/batch-delete")
    @SysLog("批量删除入校申请")
    @ApiOperation("批量删除入校申请")
    public R batchDelete(@RequestHeader("token") String token, @RequestBody Map<String, Object> params) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        List<Long> ids = (List<Long>) params.get("ids");
        applicationService.batchDelete(ids);
        return R.ok("删除成功");
    }

    /**
     * 8. 待审批申请数量统计
     */
    @GetMapping("/pending/count")
    @ApiOperation("待审批申请数量统计")
    public R pendingCount(@RequestHeader("token") String token) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        Integer count = applicationService.getPendingCount();
        return R.ok().put("pendingCount", count);
    }

    /**
     * 9. 批量审批拒绝
     */
    @PutMapping("/batch-reject")
    @SysLog("批量审批拒绝")
    @ApiOperation("批量审批拒绝")
    public R batchReject(@RequestHeader("token") String token, @RequestBody Map<String, Object> params) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        List<Long> ids = (List<Long>) params.get("ids");
        String reason = (String) params.get("reason");
        io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        applicationService.batchReject(ids, reason, userId);
        return R.ok("批量拒绝成功");
    }

    /**
     * 10. 待审批申请列表
     */
    @GetMapping("/pending/list")
    @ApiOperation("待审批申请列表")
    public R pendingList(@RequestHeader("token") String token, @RequestParam Map<String, Object> params) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        PageUtils page = applicationService.queryPendingPage(params);
        return R.ok().put("page", page);
    }

    // ==================== 用户API接口 ====================

    /**
     * 提交入校申请 (用户接口)
     */
    @PostMapping("/submit")
    @ApiOperation("提交入校申请")
    public R submit(HttpServletRequest request, @RequestBody ApplicationForm form) {
        // 1. 校验 Token
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            return R.error("token不能为空");
        }

        // 2. 检查黑名单
        Boolean isBlacklisted = stringRedisTemplate.hasKey("blacklist:" + token);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            return R.error("token已失效，请重新登录");
        }

        // 3. 解析 Token
        Claims claims = jwtUtils.getClaimByToken(token);
        if (claims == null || jwtUtils.isTokenExpired(claims.getExpiration())) {
            return R.error("token无效或已过期");
        }
        Long visitorId = Long.parseLong(claims.getSubject());

        // 4. 表单校验
        ValidatorUtils.validateEntity(form);

        // 5. 提交申请
        Long applicationId = applicationService.submit(form, visitorId);

        // 6. 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("applicationId", applicationId);
        return R.ok(data);
    }

    /**
     * 申请个人列表查询 (用户接口)
     */
    @GetMapping("/user/list")
    @ApiOperation("申请个人列表查询")
    public R userList(ApplicationQueryForm form, HttpServletRequest request) {
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

    /**
     * 申请详情查询 (用户接口)
     */
    @GetMapping("/user/detail/{id}")
    @ApiOperation("申请详情查询")
    public R userDetail(@PathVariable("id") Long id, HttpServletRequest request) {
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

    /**
     * 取消入校申请 (用户接口)
     */
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