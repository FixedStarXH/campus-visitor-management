/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package io.renren.modules.sys.controller;

import io.renren.common.utils.R;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.sys.entity.SysAdminEntity;
import io.renren.modules.sys.entity.SysUserEntity;
import io.renren.modules.sys.form.SysLoginForm;
import io.renren.modules.sys.service.SysAdminService;
import io.renren.modules.sys.service.SysCaptchaService;
import io.renren.modules.sys.service.SysUserService;
import io.renren.modules.sys.service.SysUserTokenService;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class SysLoginController {
    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysAdminService sysAdminService;
    @Autowired
    private SysUserTokenService sysUserTokenService;
    @Autowired
    private SysCaptchaService sysCaptchaService;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping("captcha.jpg")
    public void captcha(HttpServletResponse response, String uuid)throws IOException {
        response.setHeader("Cache-Control", "no-store, no-cache");
        response.setContentType("image/jpeg");

        BufferedImage image = sysCaptchaService.getCaptcha(uuid);

        ServletOutputStream out = response.getOutputStream();
        ImageIO.write(image, "jpg", out);
        IOUtils.closeQuietly(out);
    }

    @PostMapping("/sys/login")
    public Map<String, Object> login(@RequestBody SysLoginForm form)throws IOException {
        boolean captcha = sysCaptchaService.validate(form.getUuid(), form.getCaptcha());
        if(!captcha){
            return R.error("验证码不正确");
        }

        SysUserEntity user = sysUserService.queryByUserName(form.getUsername());
        SysAdminEntity admin = null;
        boolean isAdmin = false;

        if(user == null){
            admin = sysAdminService.queryByUserName(form.getUsername());
            if(admin != null){
                isAdmin = true;
            }
        }

        if(user == null && admin == null){
            return R.error("账号或密码不正确");
        }

        if(isAdmin){
            if(!admin.getPassword().equals(new Sha256Hash(form.getPassword(), admin.getSalt()).toHex())){
                return R.error("账号或密码不正确");
            }
            if(admin.getStatus() == 0){
                return R.error("账号已被锁定,请联系管理员");
            }

            String token = jwtUtils.generateToken(admin.getAdminId(), "ADMIN");
            Map<String, Object> data = R.ok();
            data.put("token", token);
            data.put("userId", admin.getAdminId());
            data.put("username", admin.getUsername());
            data.put("userType", "ADMIN");
            data.put("realName", admin.getRealName());
            return data;
        }else{
            if(!user.getPassword().equals(new Sha256Hash(form.getPassword(), user.getSalt()).toHex())){
                return R.error("账号或密码不正确");
            }
            if(user.getStatus() == 0){
                return R.error("账号已被锁定,请联系管理员");
            }

            String token = jwtUtils.generateToken(user.getUserId(), "VISITOR");
            Map<String, Object> data = R.ok();
            data.put("token", token);
            data.put("userId", user.getUserId());
            data.put("username", user.getUsername());
            data.put("userType", "VISITOR");
            return data;
        }
    }

    @PostMapping("/sys/logout")
    public R logout(@RequestHeader(value = "token", required = false) String token) {
        if(token != null && !token.isEmpty()){
            try {
                io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
                if(claims != null){
                    long ttl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                    if(ttl > 0){
                        stringRedisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.SECONDS);
                    }
                }
            } catch (Exception e) {
            }
        }
        return R.ok();
    }

    @PostMapping("/sys/password")
    public R password(@RequestHeader("token") String token, @RequestBody Map<String, String> params) {
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
        } catch (Exception e) {
            return R.error("token无效，请重新登录");
        }

        String password = params.get("password");
        String newPassword = params.get("newPassword");
        String confirmPassword = params.get("confirmPassword");

        if(password == null || newPassword == null || confirmPassword == null){
            return R.error("参数不能为空");
        }

        if(newPassword.length() < 6){
            return R.error("新密码长度不能少于6位");
        }

        if(!newPassword.matches("^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d!@#$%^&*.]{6,}$")){
            return R.error("新密码需包含字母和数字");
        }

        if(!newPassword.equals(confirmPassword)){
            return R.error("两次密码不一致");
        }

        io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
        Long userId = Long.parseLong(claims.getSubject());

        SysAdminEntity admin = sysAdminService.getById(userId);
        if(admin == null){
            return R.error("用户不存在");
        }

        String username = params.get("username");
        if(username == null || !username.equals(admin.getUsername())){
            return R.error("不能修改其他账号密码");
        }

        boolean flag = sysAdminService.updatePassword(userId, password, newPassword);
        if(!flag){
            return R.error("原密码不正确");
        }

        if(token != null && !token.isEmpty()){
            try {
                io.jsonwebtoken.Claims logoutClaims = jwtUtils.getClaimByToken(token);
                if(logoutClaims != null){
                    long ttl = (logoutClaims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
                    if(ttl > 0){
                        stringRedisTemplate.opsForValue().set("blacklist:" + token, "1", ttl, TimeUnit.SECONDS);
                    }
                }
            } catch (Exception e) {
            }
        }

        return R.ok("密码修改成功");
    }
}
