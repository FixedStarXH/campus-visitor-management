package io.renren.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;
import io.renren.modules.sys.dao.SysAdminDao;
import io.renren.modules.sys.entity.SysAdminEntity;
import io.renren.modules.sys.service.SysAdminRoleService;
import io.renren.modules.sys.service.SysAdminService;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

@Service("sysAdminService")
public class SysAdminServiceImpl extends ServiceImpl<SysAdminDao, SysAdminEntity> implements SysAdminService {
    @Autowired
    private SysAdminRoleService sysAdminRoleService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String username = (String)params.get("username");
        String realName = (String)params.get("realName");

        IPage<SysAdminEntity> page = this.page(
            new Query<SysAdminEntity>().getPage(params),
            new QueryWrapper<SysAdminEntity>()
                .like(StringUtils.isNotBlank(username), "username", username)
                .like(StringUtils.isNotBlank(realName), "real_name", realName)
                .eq("deleted", 0)
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveAdmin(SysAdminEntity admin) {
        admin.setCreateTime(new Date());
        String salt = RandomStringUtils.randomAlphanumeric(20);
        admin.setSalt(salt);
        admin.setPassword(new Sha256Hash(admin.getPassword(), salt).toHex());
        this.save(admin);
        
        sysAdminRoleService.saveOrUpdate(admin.getAdminId(), admin.getRoleIdList());
    }

    @Override
    @Transactional
    public void update(SysAdminEntity admin) {
        if(StringUtils.isBlank(admin.getPassword())){
            admin.setPassword(null);
        }else{
            admin.setPassword(new Sha256Hash(admin.getPassword(), admin.getSalt()).toHex());
        }
        this.updateById(admin);
        
        sysAdminRoleService.saveOrUpdate(admin.getAdminId(), admin.getRoleIdList());
    }

    @Override
    public void deleteBatch(Long[] adminIds) {
        for(Long adminId : adminIds){
            SysAdminEntity admin = new SysAdminEntity();
            admin.setAdminId(adminId);
            admin.setDeleted(1);
            this.updateById(admin);
        }
    }

    @Override
    public Map<String, Object> checkPromoteCondition(Long userId) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("canPromote", true);
        result.put("reasons", new java.util.ArrayList<>());
        return result;
    }

    @Override
    @Transactional
    public void promoteToAdmin(Long visitorId, SysAdminEntity admin) {
        admin.setSource(1);
        admin.setSourceVisitorId(visitorId);
        admin.setPromoteTime(new Date());
        this.saveAdmin(admin);
    }

    @Override
    public SysAdminEntity queryByUserName(String username) {
        return this.getOne(new QueryWrapper<SysAdminEntity>().eq("username", username));
    }

    @Override
    public SysAdminEntity queryByPhone(String phone) {
        return this.getOne(new QueryWrapper<SysAdminEntity>().eq("phone", phone).last("limit 1"));
    }

    @Override
    public SysAdminEntity queryByEmail(String email) {
        return this.getOne(new QueryWrapper<SysAdminEntity>().eq("email", email).last("limit 1"));
    }

    @Override
    public SysAdminEntity queryByPhoneExcludeId(String phone, Long excludeId) {
        return this.getOne(new QueryWrapper<SysAdminEntity>()
                .eq("phone", phone)
                .ne("admin_id", excludeId)
                .last("limit 1"));
    }

    @Override
    public SysAdminEntity queryByEmailExcludeId(String email, Long excludeId) {
        return this.getOne(new QueryWrapper<SysAdminEntity>()
                .eq("email", email)
                .ne("admin_id", excludeId)
                .last("limit 1"));
    }

    @Override
    public boolean updatePassword(Long adminId, String password, String newPassword) {
        SysAdminEntity admin = this.getById(adminId);
        if(admin == null){
            return false;
        }
        String encryptedPassword = new Sha256Hash(password, admin.getSalt()).toHex();
        if(!encryptedPassword.equals(admin.getPassword())){
            return false;
        }
        String encryptedNewPassword = new Sha256Hash(newPassword, admin.getSalt()).toHex();
        admin.setPassword(encryptedNewPassword);
        return this.updateById(admin);
    }
}
