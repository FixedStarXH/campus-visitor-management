package io.renren.modules.ers.controller;

import io.renren.common.annotation.SysLog;
import io.renren.common.utils.Constant;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.R;
import io.renren.common.validator.Assert;
import io.renren.common.validator.ValidatorUtils;
import io.renren.common.validator.group.AddGroup;
import io.renren.common.validator.group.UpdateGroup;
import io.renren.modules.ers.entity.UserEntity;
import io.renren.modules.ers.service.UserService;
import org.apache.commons.lang.ArrayUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    @RequiresPermissions("admin:user:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = userService.queryPage(params);
        return R.ok().put("page", page);
    }

    @GetMapping("/info/{userId}")
    @RequiresPermissions("admin:user:info")
    public R info(@PathVariable("userId") Long userId) {
        UserEntity user = userService.getById(userId);
        return R.ok().put("user", user);
    }

    @SysLog("保存用户")
    @PostMapping("/save")
    @RequiresPermissions("admin:user:save")
    public R save(@RequestBody UserEntity user) {
        ValidatorUtils.validateEntity(user, AddGroup.class);
        userService.saveUser(user);
        return R.ok();
    }

    @SysLog("修改用户")
    @PostMapping("/update")
    @RequiresPermissions("admin:user:update")
    public R update(@RequestBody UserEntity user) {
        ValidatorUtils.validateEntity(user, UpdateGroup.class);
        userService.updateUser(user);
        return R.ok();
    }

    @SysLog("删除用户")
    @PostMapping("/delete")
    @RequiresPermissions("admin:user:delete")
    public R delete(@RequestBody Long[] userIds) {
        if (ArrayUtils.contains(userIds, 1L)) {
            return R.error("系统管理员不能删除");
        }
        userService.deleteBatch(userIds);
        return R.ok();
    }

    @SysLog("修改用户状态")
    @GetMapping("/status")
    @RequiresPermissions("admin:user:update")
    public R updateStatus(@RequestParam Long userId, @RequestParam Integer status) {
        Assert.isNull(userId, "用户ID不能为空");
        Assert.isNull(status, "状态不能为空");
        userService.updateStatus(userId, status);
        return R.ok();
    }
}
