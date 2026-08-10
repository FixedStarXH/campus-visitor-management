/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 *
 * https://www.renren.io
 *
 * 版权所有，侵权必究！
 */

package io.renren.modules.sys.service.impl;

import io.renren.common.utils.Constant;
import io.renren.modules.sys.dao.SysAdminDao;
import io.renren.modules.sys.dao.SysMenuDao;
import io.renren.modules.sys.dao.SysUserDao;
import io.renren.modules.sys.dao.SysUserTokenDao;
import io.renren.modules.sys.entity.SysAdminEntity;
import io.renren.modules.sys.entity.SysMenuEntity;
import io.renren.modules.sys.entity.SysUserEntity;
import io.renren.modules.sys.entity.SysUserTokenEntity;
import io.renren.modules.sys.service.ShiroService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ShiroServiceImpl implements ShiroService {
    @Autowired
    private SysMenuDao sysMenuDao;
    @Autowired
    private SysUserDao sysUserDao;
    @Autowired
    private SysUserTokenDao sysUserTokenDao;
    @Autowired
    private SysAdminDao sysAdminDao;

    @Override
    public Set<String> getUserPermissions(long userId) {
        List<String> permsList;

        if(userId == Constant.SUPER_ADMIN){
            List<SysMenuEntity> menuList = sysMenuDao.selectList(null);
            permsList = new ArrayList<>(menuList.size());
            for(SysMenuEntity menu : menuList){
                permsList.add(menu.getPerms());
            }
        }else{
            SysUserEntity user = sysUserDao.selectById(userId);
            if(user == null){
                SysAdminEntity admin = sysAdminDao.selectById(userId);
                if(admin != null){
                    List<SysMenuEntity> menuList = sysMenuDao.selectList(null);
                    permsList = new ArrayList<>(menuList.size());
                    for(SysMenuEntity menu : menuList){
                        permsList.add(menu.getPerms());
                    }
                }else{
                    permsList = new ArrayList<>();
                }
            }else{
                permsList = sysUserDao.queryAllPerms(userId);
            }
        }

        Set<String> permsSet = new HashSet<>();
        for(String perms : permsList){
            if(StringUtils.isBlank(perms)){
                continue;
            }
            permsSet.addAll(Arrays.asList(perms.trim().split(",")));
        }
        return permsSet;
    }

    @Override
    public SysUserTokenEntity queryByToken(String token) {
        return sysUserTokenDao.queryByToken(token);
    }

    @Override
    public SysUserEntity queryUser(Long userId) {
        SysUserEntity user = sysUserDao.selectById(userId);
        if(user != null){
            return user;
        }
        SysAdminEntity admin = sysAdminDao.selectById(userId);
        if(admin != null){
            user = new SysUserEntity();
            user.setUserId(admin.getAdminId());
            user.setUsername(admin.getUsername());
            user.setPassword(admin.getPassword());
            user.setSalt(admin.getSalt());
            user.setStatus(admin.getStatus());
            return user;
        }
        return null;
    }
}
