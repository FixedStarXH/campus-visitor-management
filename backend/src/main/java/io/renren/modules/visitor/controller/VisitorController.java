package io.renren.modules.visitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.jsonwebtoken.Claims;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.ers.entity.ErsUserEntity;
import io.renren.modules.ers.service.ErsUserService;
import io.renren.modules.sys.entity.SysAdminEntity;
import io.renren.modules.sys.service.SysAdminService;
import io.renren.modules.sys.service.SysCaptchaService;
import io.renren.modules.visitor.form.LoginForm;
import io.renren.modules.visitor.form.PasswordForm;
import io.renren.modules.visitor.form.RegisterForm;
import io.renren.modules.visitor.form.UpdateForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
@Api("用户接口")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class VisitorController {

    @Autowired
    private ErsUserService ersUserService;
    @Autowired
    private SysAdminService sysAdminService;
    @Autowired
    private SysCaptchaService sysCaptchaService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/register")
    @ApiOperation("用户注册")
    public R register(@RequestBody RegisterForm form) {
        ValidatorUtils.validateEntity(form);

        boolean captchaValid = sysCaptchaService.validate(form.getUuid(), form.getCaptcha());
        if (!captchaValid) {
            return R.error("验证码错误");
        }

        QueryWrapper<ErsUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", form.getUsername()).or().eq("mobile", form.getMobile());
        ErsUserEntity existUser = ersUserService.getOne(queryWrapper);
        if (existUser != null) {
            return R.error("用户名或手机号已存在");
        }

        String salt = RandomStringUtils.randomAlphanumeric(20);
        String password = new Sha256Hash(form.getPassword(), salt).toHex();

        ErsUserEntity user = new ErsUserEntity();
        user.setUsername(form.getUsername());
        user.setAccount(form.getUsername());
        user.setPassword(password);
        user.setSalt(salt);
        user.setMobile(form.getMobile());
        user.setEmail(form.getEmail());
        user.setRealName(form.getRealName());
        user.setGender(form.getGender());
        user.setUserType("VISITOR");
        user.setStatus(1);
        user.setBlacklistFlag(0);
        user.setCreateTime(new Date());
        user.setRegisterTime(new Date());

        ersUserService.save(user);
        return R.ok("注册成功");
    }

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public R login(@RequestBody LoginForm form) {
        boolean captchaValid = sysCaptchaService.validate(form.getUuid(), form.getCaptcha());
        if (!captchaValid) {
            return R.error("验证码错误");
        }

        QueryWrapper<ErsUserEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", form.getUsername()).or().eq("mobile", form.getUsername());
        ErsUserEntity user = ersUserService.getOne(queryWrapper);
        SysAdminEntity admin = null;
        boolean isAdmin = false;

        if (user == null) {
            admin = sysAdminService.queryByUserName(form.getUsername());
            if (admin != null) {
                isAdmin = true;
            }
        }

        if (user == null && admin == null) {
            return R.error("账号或密码不正确");
        }

        if (isAdmin) {
            if (!admin.getPassword().equals(new Sha256Hash(form.getPassword(), admin.getSalt()).toHex())) {
                return R.error("账号或密码不正确");
            }
            if (admin.getStatus() != null && admin.getStatus() == 0) {
                return R.error("账号已被锁定,请联系管理员");
            }

            String token = jwtUtils.generateToken(admin.getAdminId(), "ADMIN");
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", admin.getAdminId());
            data.put("username", admin.getUsername());
            data.put("userType", "ADMIN");
            data.put("realName", admin.getRealName());

            return R.ok(data);
        } else {
            if (!user.getPassword().equals(new Sha256Hash(form.getPassword(), user.getSalt()).toHex())) {
                return R.error("账号或密码不正确");
            }
            if (user.getBlacklistFlag() != null && user.getBlacklistFlag() == 1) {
                return R.error("账号已被加入黑名单");
            }
            if (user.getStatus() != null && user.getStatus() == 0) {
                return R.error("账号已被禁用");
            }

            String userType = user.getUserType() != null ? user.getUserType() : "VISITOR";
            String token = jwtUtils.generateToken(user.getUserId(), userType);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", user.getUserId());
            data.put("username", user.getUsername());
            data.put("userType", userType);

            user.setLastLoginTime(new Date());
            ersUserService.updateById(user);

            return R.ok(data);
        }
    }

    @PostMapping("/logout")
    @ApiOperation("退出登录")
    public R logout(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            return R.ok();
        }

        try {
            Claims claims = jwtUtils.getClaimByToken(token);
            if (claims != null) {
                Date expiration = claims.getExpiration();
                long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
                if (ttl > 0) {
                    stringRedisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.SECONDS);
                }
            }
        } catch (Exception e) {
        }

        return R.ok();
    }

    @PostMapping("/auth/refresh")
    @ApiOperation("刷新Token")
    public R refresh(HttpServletRequest request) {
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
            return R.error("token已过期，请重新登录");
        }

        Long userId = Long.parseLong(claims.getSubject());
        String userType = claims.get("userType", String.class);
        String newToken = jwtUtils.generateToken(userId, userType);

        Map<String, Object> data = new HashMap<>();
        data.put("token", newToken);
        data.put("userType", userType);
        return R.ok(data);
    }

    @GetMapping("/auth/verify")
    @ApiOperation("验证Token")
    public R verify(HttpServletRequest request) {
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

        Long userId = Long.parseLong(claims.getSubject());
        String userType = claims.get("userType", String.class);

        Map<String, Object> data = new HashMap<>();
        data.put("valid", true);
        data.put("userId", userId);
        data.put("userType", userType);
        return R.ok(data);
    }

    @GetMapping("/info")
    @ApiOperation("获取个人信息")
    public R info(HttpServletRequest request) {
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
        Long userId = Long.parseLong(claims.getSubject());

        ErsUserEntity user = ersUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("mobile", desensitizePhone(user.getMobile()));
        data.put("email", user.getEmail());
        data.put("userType", user.getUserType());
        data.put("avatar", user.getAvatar());
        data.put("gender", user.getGender() != null && user.getGender() == 0 ? "女" : "男");
        data.put("createTime", user.getCreateTime());

        return R.ok(data);
    }

    private String desensitizePhone(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }

    @PostMapping("/update")
    @ApiOperation("修改个人信息")
    public R update(HttpServletRequest request, @RequestBody UpdateForm form) {
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
        Long userId = Long.parseLong(claims.getSubject());

        ErsUserEntity user = ersUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        if (form.getMobile() != null && !form.getMobile().equals(user.getMobile())) {
            QueryWrapper<ErsUserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("mobile", form.getMobile());
            if (ersUserService.getOne(queryWrapper) != null) {
                return R.error("手机号已被使用");
            }
            user.setMobile(form.getMobile());
        }

        if (form.getEmail() != null && !form.getEmail().equals(user.getEmail())) {
            QueryWrapper<ErsUserEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("email", form.getEmail());
            if (ersUserService.getOne(queryWrapper) != null) {
                return R.error("邮箱已被使用");
            }
            user.setEmail(form.getEmail());
        }

        if (form.getRealName() != null) {
            user.setRealName(form.getRealName());
        }
        if (form.getAvatar() != null) {
            user.setAvatar(form.getAvatar());
        }
        if (form.getGender() != null) {
            user.setGender(form.getGender());
        }

        ersUserService.updateById(user);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("mobile", desensitizePhone(user.getMobile()));
        data.put("email", user.getEmail());
        data.put("userType", user.getUserType());
        data.put("avatar", user.getAvatar());
        data.put("gender", user.getGender());

        return R.ok(data);
    }

    @PostMapping("/password")
    @ApiOperation("修改密码")
    public R updatePassword(HttpServletRequest request, @RequestBody PasswordForm form) {
        ValidatorUtils.validateEntity(form);
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
        Long userId = Long.parseLong(claims.getSubject());

        ErsUserEntity user = ersUserService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }

        String oldEncrypt = new Sha256Hash(form.getOldPassword(), user.getSalt()).toHex();
        if (!oldEncrypt.equals(user.getPassword())) {
            return R.error("旧密码错误");
        }

        if (form.getOldPassword().equals(form.getNewPassword())) {
            return R.error("新密码不能与旧密码相同");
        }

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            return R.error("两次输入的新密码不一致");
        }

        String newSalt = RandomStringUtils.randomAlphanumeric(20);
        String newPassword = new Sha256Hash(form.getNewPassword(), newSalt).toHex();

        user.setSalt(newSalt);
        user.setPassword(newPassword);
        ersUserService.updateById(user);

        Date expiration = claims.getExpiration();
        long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        if (ttl > 0) {
            stringRedisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.SECONDS);
        }

        return R.ok("密码修改成功，请重新登录");
    }
}