package io.renren.modules.sys.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.R;
import io.renren.common.validator.ValidatorUtils;
import io.renren.common.validator.group.AddGroup;
import io.renren.common.validator.group.UpdateGroup;
import io.renren.modules.app.utils.JwtUtils;
import io.renren.modules.sys.entity.SysAdminEntity;
import io.renren.modules.sys.entity.SysUserEntity;
import io.renren.modules.sys.service.SysAdminRoleService;
import io.renren.modules.sys.service.SysAdminService;
import io.renren.modules.sys.service.SysUserService;
import io.renren.modules.ers.service.ErsUserService;
import io.renren.modules.ers.entity.ErsUserEntity;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理员管理 (ERS 独立方案)
 */
@RestController
@RequestMapping("/admin/manager")
public class SysManagerController extends AbstractController {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SysAdminService sysAdminService;
    @Autowired
    private SysAdminRoleService sysAdminRoleService;
    @Autowired
    private SysUserService sysUserService;
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
            if(claims == null){
                return R.error("token无效，请重新登录");
            }
            if(jwtUtils.isTokenExpired(claims.getExpiration())){
                return R.error("token失效，请重新登录");
            }
            String userType = claims.get("userType", String.class);
            if(!"ADMIN".equals(userType)){
                return R.error("无权限操作，需要管理员权限");
            }
            return null;
        } catch (Exception e) {
            logger.error("Token验证异常", e);
            return R.error("token无效，请重新登录");
        }
    }

    /**
     * 5. 管理员列表
     */
    @GetMapping("/list")
    public R list(@RequestHeader("token") String token, @RequestParam Map<String, Object> params){
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        PageUtils page = sysAdminService.queryPage(params);
        return R.ok().put("page", page);
    }

    /**
     * 7. 修改管理员
     */
    @SysLog("修改管理员")
    @PutMapping("/update/{id}")
    public R update(@RequestHeader("token") String token, @PathVariable("id") Long id, @RequestBody SysAdminEntity admin){
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        io.jsonwebtoken.Claims claims = jwtUtils.getClaimByToken(token);
        Long userId = Long.parseLong(claims.getSubject());

        SysAdminEntity existAdmin = sysAdminService.getById(id);
        if(existAdmin == null || existAdmin.getDeleted() != null && existAdmin.getDeleted() == 1){
            return R.error("该管理员不存在");
        }

        if(admin.getUsername() != null && admin.getUsername().equals(existAdmin.getUsername()) &&
           (admin.getPhone() == null ? existAdmin.getPhone() == null : admin.getPhone().equals(existAdmin.getPhone())) &&
           (admin.getEmail() == null ? existAdmin.getEmail() == null : admin.getEmail().equals(existAdmin.getEmail())) &&
           (admin.getStatus() == null ? existAdmin.getStatus() == null : admin.getStatus().equals(existAdmin.getStatus()))){
            return R.error("信息相同，无需修改");
        }

        ValidatorUtils.validateEntity(admin, UpdateGroup.class);

        if(admin.getPhone() != null && !admin.getPhone().isEmpty()){
            SysAdminEntity existPhone = sysAdminService.queryByPhoneExcludeId(admin.getPhone(), id);
            if(existPhone != null){
                return R.error("手机号已存在");
            }
        }

        if(admin.getEmail() != null && !admin.getEmail().isEmpty()){
            SysAdminEntity existEmail = sysAdminService.queryByEmailExcludeId(admin.getEmail(), id);
            if(existEmail != null){
                return R.error("邮箱已存在");
            }
        }

        admin.setAdminId(id);
        admin.setCreateUserId(userId);
        sysAdminService.update(admin);
        return R.ok();
    }

    /**
     * 8. 删除管理员
     */
    @SysLog("删除管理员")
    @DeleteMapping("/delete/{id}")
    public R delete(@RequestHeader("token") String token, @PathVariable("id") Long id){
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        if(id == 1L) return R.error("系统管理员不能删除");

        SysAdminEntity admin = sysAdminService.getById(id);
        if(admin == null || admin.getDeleted() != null && admin.getDeleted() == 1){
            return R.error("该管理员不存在");
        }

        sysAdminService.deleteBatch(new Long[]{id});
        return R.ok();
    }

    /**
     * 9. 启用/禁用管理员
     */
    @SysLog("启用禁用管理员")
    @PutMapping("/status/{id}")
    public R status(@RequestHeader("token") String token, @PathVariable("id") Long id, @RequestBody Map<String, Integer> params){
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        SysAdminEntity existAdmin = sysAdminService.getById(id);
        if(existAdmin == null || existAdmin.getDeleted() != null && existAdmin.getDeleted() == 1){
            return R.error("该账号不存在");
        }

        Integer status = params.get("status");
        if(status == null){
            return R.error("状态值(status)不能为空");
        }
        if(status != 0 && status != 1){
            return R.error("状态值只能为0或1");
        }
        if(id == 1L && status == 0) return R.error("超级管理员不能被禁用");

        Integer currentStatus = existAdmin.getStatus();
        if(status == 0 && currentStatus != null && currentStatus == 0){
            return R.error("该账号已被禁用");
        }
        if(status == 1 && currentStatus != null && currentStatus == 1){
            return R.error("该账号已启用");
        }

        SysAdminEntity admin = new SysAdminEntity();
        admin.setAdminId(id);
        admin.setStatus(status);
        sysAdminService.updateById(admin);

        if(status == 0){
            return R.ok("禁用成功");
        }else{
            return R.ok("启用成功");
        }
    }

    /**
     * 管理员详情
     */
    @GetMapping("/info/{id}")
    public R info(@RequestHeader("token") String token, @PathVariable("id") Long id){
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        SysAdminEntity admin = sysAdminService.getById(id);
        if(admin == null || admin.getDeleted() != null && admin.getDeleted() == 1){
            return R.error("该管理员不存在");
        }
        List<Long> roleIdList = sysAdminRoleService.queryRoleIdList(id);
        admin.setRoleIdList(roleIdList);
        return R.ok().put("admin", admin);
    }

    /**
     * 黑名单操作 (PUT)
     * 参数：{userId: 1, action: "add"/"remove", reason: "爽约3次"}
     */
    @SysLog("黑名单操作")
    @PutMapping("/blacklist")
    public R blacklist(@RequestHeader("token") String token, @RequestBody Map<String, Object> params){
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        Long userId = Long.valueOf(params.get("userId").toString());
        String action = (String) params.get("action");
        String reason = params.get("reason") != null ? (String) params.get("reason") : "";

        ErsUserEntity user = ersUserService.getById(userId);
        if(user == null){
            return R.error("该账号不存在");
        }

        if("add".equals(action)){
            if(user.getBlacklistFlag() != null && user.getBlacklistFlag() == 1){
                return R.error("该账号已在黑名单中");
            }
            ersUserService.addToBlacklist(userId, reason, null);
            return R.ok("移入黑名单成功");
        }else if("remove".equals(action)){
            if(user.getBlacklistFlag() == null || user.getBlacklistFlag() != 1){
                return R.error("该账号不在黑名单中");
            }
            ersUserService.removeFromBlacklist(userId);
            return R.ok("移出黑名单成功");
        }

        return R.error("无效的操作类型");
    }

    /**
     * 用户提升条件检查 (GET)
     */
    @GetMapping("/promote/check/{userId}")
    public R promoteCheck(@RequestHeader("token") String token, @PathVariable("userId") Long userId){
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        ErsUserEntity user = ersUserService.getById(userId);
        if(user == null){
            return R.error("该账号不存在");
        }

        if(user.getBlacklistFlag() != null && user.getBlacklistFlag() == 1){
            return R.error("该账号在黑名单中，无法提升");
        }

        if(user.getIsPromoted() != null && user.getIsPromoted() == 1){
            return R.error("该账号已是管理员");
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("canPromote", true);
        result.put("reasons", new java.util.ArrayList<>());
        return R.ok().put("data", result);
    }

    /**
     * 用户提升为管理员 (POST)
     */
    @SysLog("用户提升为管理员")
    @PostMapping("/promote")
    public R promote(@RequestHeader("token") String token, @RequestBody Map<String, Object> params){
        R verifyResult = verifyToken(token);
        if(verifyResult != null) return verifyResult;

        Long sourceUserId = Long.valueOf(params.get("sourceVisitorId").toString());
        String username = (String) params.get("username");
        List<Long> roleIdList = (List<Long>) params.get("roleIdList");

        if(sourceUserId == null){
            return R.error("用户ID不能为空");
        }
        if(username == null || username.isEmpty()){
            return R.error("用户名不能为空");
        }

        ErsUserEntity user = ersUserService.getById(sourceUserId);
        if(user == null){
            return R.error("该用户ID不存在");
        }

        if(!username.equals(user.getUsername())){
            return R.error("用户名与用户ID不匹配");
        }

        if(user.getBlacklistFlag() != null && user.getBlacklistFlag() == 1){
            return R.error("该账号在黑名单中，无法提升");
        }

        if(user.getIsPromoted() != null && user.getIsPromoted() == 1){
            return R.error("该账号已是管理员");
        }

        String realName = user.getRealName();

        SysAdminEntity existAdmin = sysAdminService.queryByUserName(username);
        if (existAdmin != null) {
            return R.error("管理员用户名已存在");
        }

        SysAdminEntity admin = new SysAdminEntity();
        admin.setUsername(username);
        admin.setPassword(user.getPassword());
        admin.setSalt(user.getSalt());
        admin.setRealName(realName);
        admin.setPhone(user.getMobile());
        admin.setEmail(user.getEmail());
        admin.setStatus(1);
        admin.setSource(1);
        admin.setSourceVisitorId(sourceUserId);
        admin.setPromoteTime(new Date());
        admin.setCreateTime(new Date());

        sysAdminService.save(admin);

        SysAdminEntity savedAdmin = sysAdminService.queryByUserName(username);
        if(savedAdmin != null && roleIdList != null && !roleIdList.isEmpty()){
            List<Long> longRoleIdList = new ArrayList<>();
            for(Number n : roleIdList){
                longRoleIdList.add(n.longValue());
            }
            sysAdminRoleService.saveOrUpdate(savedAdmin.getAdminId(), longRoleIdList);
        }

        // 从ers_user中删除
        ersUserService.removeById(sourceUserId);

        return R.ok("提升成功");
    }
}
