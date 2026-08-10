package io.renren.modules.ers.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.Constant;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.R;
import io.renren.common.validator.Assert;
import io.renren.common.validator.ValidatorUtils;
import io.renren.common.validator.group.AddGroup;
import io.renren.common.validator.group.UpdateGroup;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.ers.entity.ErsUserEntity;
import io.jsonwebtoken.Claims;
import io.renren.modules.ers.service.ErsUserService;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 *
 * @author ERS System
 */
@RestController
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private ErsUserService ersUserService;
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

    /**
     * 用户列表
     */
    @GetMapping("/list")
    public R list(@RequestHeader("token") String token, @RequestParam Map<String, Object> params) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        PageUtils page = ersUserService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 用户详情
     */
    @GetMapping("/info/{userId}")
    public R info(@RequestHeader("token") String token, @PathVariable("userId") Long userId) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        ErsUserEntity user = ersUserService.getById(userId);
        return R.ok().put("user", user);
    }

    /**
     * 保存用户（新增）
     */
    @SysLog("保存用户")
    @PostMapping("/save")
    public R save(@RequestHeader("token") String token, @RequestBody ErsUserEntity user) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        ValidatorUtils.validateEntity(user, AddGroup.class);
        ersUserService.saveUser(user, userId);
        return R.ok();
    }

    /**
     * 删除用户
     */
    @SysLog("删除用户")
    @PostMapping("/delete")
    public R delete(@RequestHeader("token") String token, @RequestBody Map<String, Object> params) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        Object userIdsObj = params.get("userIds");
        if (userIdsObj == null) {
            return R.error("userIds不能为空");
        }

        Long[] userIds;
        if (userIdsObj instanceof List) {
            List<?> list = (List<?>) userIdsObj;
            userIds = new Long[list.size()];
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof Number) {
                    userIds[i] = ((Number) item).longValue();
                } else {
                    userIds[i] = Long.parseLong(item.toString());
                }
            }
        } else if (userIdsObj instanceof Long[]) {
            userIds = (Long[]) userIdsObj;
        } else {
            return R.error("userIds格式错误");
        }

        if (ArrayUtils.contains(userIds, 1L)) {
            return R.error("系统管理员不能删除");
        }
        ersUserService.deleteBatch(userIds);
        return R.ok();
    }

    /**
     * 修改用户
     */
    @SysLog("修改用户")
    @PostMapping("/update")
    public R update(@RequestHeader("token") String token, @RequestBody ErsUserEntity user) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        ValidatorUtils.validateEntity(user, UpdateGroup.class);
        ersUserService.updateUser(user);
        return R.ok();
    }

    /**
     * 修改用户状态
     */
    @SysLog("修改用户状态")
    @GetMapping("/status")
    public R updateStatus(@RequestHeader("token") String token, @RequestParam Long userId, @RequestParam Integer status) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        Assert.isNull(userId, "用户ID不能为空");
        Assert.isNull(status, "状态不能为空");
        ersUserService.updateStatus(userId, status);
        return R.ok();
    }

    /**
     * 加入黑名单
     */
    @SysLog("加入黑名单")
    @PostMapping("/blacklist/add")
    public R addToBlacklist(@RequestHeader("token") String token,
                           @RequestParam Long userId,
                           @RequestParam String reason,
                           @RequestParam(required = false) Date expireTime) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        ersUserService.addToBlacklist(userId, reason, expireTime);
        return R.ok();
    }

    /**
     * 移出黑名单
     */
    @SysLog("移出黑名单")
    @PostMapping("/blacklist/remove")
    public R removeFromBlacklist(@RequestHeader("token") String token, @RequestParam Long userId) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        ersUserService.removeFromBlacklist(userId);
        return R.ok();
    }

    /**
     * 检查用户提升资格
     */
    @GetMapping("/promotion/check")
    public R checkPromotionEligibility(@RequestHeader("token") String token, @RequestParam Long userId) {
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;
        boolean eligible = ersUserService.checkPromotionEligibility(userId);
        return R.ok().put("eligible", eligible);
    }
}