package io.renren.modules.visitor.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.common.exception.RRException;
import io.renren.modules.sys.entity.SysAdminEntity;
import io.renren.modules.sys.service.SysAdminService;
import io.renren.modules.ers.service.ErsConfigService;
import io.renren.modules.ers.dto.NoShowRuleDTO;
import io.renren.modules.visitor.dao.VisitorDao;
import io.renren.modules.visitor.entity.VisitorEntity;
import io.renren.modules.visitor.form.LoginForm;
import io.renren.modules.visitor.form.VisitorRegisterForm;
import io.renren.modules.visitor.service.VisitorService;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("visitorService")
public class VisitorServiceImpl extends ServiceImpl<VisitorDao, VisitorEntity> implements VisitorService {

    private static final String[] SENSITIVE_WORDS = {"admin", "root", "test", "fuck", "shit", "傻", "笨", "智障"};

    @Autowired
    private SysAdminService sysAdminService;

    @Autowired
    private ErsConfigService ersConfigService;

    @Override
    public void register(VisitorRegisterForm form) {
        if (this.count(new QueryWrapper<VisitorEntity>().eq("username", form.getUsername())) > 0) {
            throw new RRException("用户名已被注册");
        }

        if (this.count(new QueryWrapper<VisitorEntity>().eq("phone", form.getPhone())) > 0) {
            throw new RRException("手机号已被注册");
        }

        if (form.getEmail() != null && !form.getEmail().isEmpty()) {
            if (this.count(new QueryWrapper<VisitorEntity>().eq("email", form.getEmail())) > 0) {
                throw new RRException("邮箱已被注册");
            }
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new RRException("两次输入的密码不一致");
        }

        checkSensitiveWords(form.getUsername());

        VisitorEntity visitor = new VisitorEntity();
        visitor.setUsername(form.getUsername());
        visitor.setAccount(form.getUsername());
        visitor.setPhone(form.getPhone());
        visitor.setEmail(form.getEmail());
        visitor.setGender(form.getGender() != null ? form.getGender() : 1);

        String salt = RandomStringUtils.randomAlphanumeric(20);
        visitor.setSalt(salt);
        visitor.setPassword(new Sha256Hash(form.getPassword(), salt).toHex());

        visitor.setUserType("VISITOR");
        visitor.setStatus(0);
        visitor.setBlacklistStatus(0);
        visitor.setPromotedToAdmin(0);
        visitor.setNoShowCount(0);
        visitor.setAvatar("/default-avatar.png");
        visitor.setCreateTime(new Date());

        this.save(visitor);
    }

    @Override
    public Map<String, Object> login(LoginForm form) {
        VisitorEntity user = this.getOne(
                new QueryWrapper<VisitorEntity>()
                        .eq("username", form.getUsername())
                        .or()
                        .eq("phone", form.getUsername())
        );

        if (user == null) {
            throw new RRException("用户不存在");
        }

        String password = new Sha256Hash(form.getPassword(), user.getSalt()).toHex();
        if (!password.equals(user.getPassword())) {
            throw new RRException("密码错误");
        }

        if (user.getStatus() == 2) {
            throw new RRException("账号已被加入黑名单");
        }
        if (user.getStatus() != 0) {
            throw new RRException("账号已被禁用");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("userType", user.getUserType());
        result.put("phone", user.getPhone());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToBlacklist(Long visitorId, String reason) {
        VisitorEntity visitor = this.getById(visitorId);
        if (visitor == null) {
            throw new RRException("访客不存在");
        }
        if (visitor.getPromotedToAdmin() != null && visitor.getPromotedToAdmin() == 1) {
            throw new RRException("该用户已是管理员，无法加入黑名单");
        }

        visitor.setStatus(2);
        visitor.setBlacklistStatus(1);
        visitor.setBlacklistReason(reason);
        visitor.setBlacklistEndTime(null);
        visitor.setUpdateTime(new Date());
        this.updateById(visitor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFromBlacklist(Long visitorId) {
        VisitorEntity visitor = this.getById(visitorId);
        if (visitor == null) {
            throw new RRException("访客不存在");
        }

        visitor.setStatus(1);
        visitor.setBlacklistStatus(0);
        visitor.setBlacklistReason(null);
        visitor.setNoShowCount(0);
        visitor.setUpdateTime(new Date());
        this.updateById(visitor);
    }

    @Override
    public Map<String, Object> checkPromoteCondition(Long visitorId) {
        Map<String, Object> result = new HashMap<>();
        List<String> reasons = new ArrayList<>();
        boolean canPromote = true;

        VisitorEntity visitor = this.getById(visitorId);
        if (visitor == null) {
            result.put("canPromote", false);
            reasons.add("访客不存在");
            result.put("reasons", reasons);
            return result;
        }

        if (visitor.getPromotedToAdmin() != null && visitor.getPromotedToAdmin() == 1) {
            canPromote = false;
            reasons.add("该用户已是管理员");
        }

        if (visitor.getStatus() == 2) {
            canPromote = false;
            reasons.add("该用户在黑名单中");
        }

        if (visitor.getNoShowCount() != null && visitor.getNoShowCount() >= 3) {
            canPromote = false;
            reasons.add("爽约次数过多");
        }

        if (visitor.getAuthStatus() == null || visitor.getAuthStatus() != 1) {
            reasons.add("未完成实名认证");
        }

        result.put("canPromote", canPromote);
        result.put("reasons", reasons);
        result.put("visitor", visitor);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void promoteToAdmin(Long visitorId, String username, String password, String realName, String phone) {
        VisitorEntity visitor = this.getById(visitorId);
        if (visitor == null) {
            throw new RRException("访客不存在");
        }
        if (visitor.getPromotedToAdmin() != null && visitor.getPromotedToAdmin() == 1) {
            throw new RRException("该用户已是管理员");
        }

        SysAdminEntity existAdmin = sysAdminService.queryByUserName(username);
        if (existAdmin != null) {
            throw new RRException("管理员用户名已存在");
        }

        SysAdminEntity admin = new SysAdminEntity();
        admin.setUsername(username);
        admin.setPassword(password);
        admin.setRealName(realName);
        admin.setPhone(phone != null ? phone : visitor.getPhone());
        admin.setEmail(visitor.getEmail());
        admin.setStatus(1);
        admin.setSource(1);
        admin.setSourceVisitorId(visitorId);
        admin.setPromoteTime(new Date());

        sysAdminService.saveAdmin(admin);

        visitor.setPromotedToAdmin(1);
        visitor.setAdminId(admin.getAdminId());
        visitor.setUserType("ADMIN");
        visitor.setUpdateTime(new Date());
        this.updateById(visitor);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void promoteToAdminSimple(Long visitorId, String username, String realName, List<Long> roleIdList) {
        VisitorEntity visitor = this.getById(visitorId);
        if (visitor == null) {
            throw new RRException("该账号不存在");
        }
        if (visitor.getStatus() != null && visitor.getStatus() == 2) {
            throw new RRException("该账号在黑名单中，无法提升");
        }
        if (visitor.getAuthStatus() == null || visitor.getAuthStatus() != 1) {
            throw new RRException("该账号未完成实名认证，无法提升");
        }
        if (visitor.getPromotedToAdmin() != null && visitor.getPromotedToAdmin() == 1) {
            throw new RRException("该用户已是管理员");
        }

        SysAdminEntity existAdmin = sysAdminService.queryByUserName(username);
        if (existAdmin != null) {
            throw new RRException("管理员用户名已存在");
        }

        String randomPassword = RandomStringUtils.randomAlphanumeric(16);
        String salt = RandomStringUtils.randomAlphanumeric(20);
        String encryptedPassword = new Sha256Hash(randomPassword, salt).toHex();

        SysAdminEntity admin = new SysAdminEntity();
        admin.setUsername(username);
        admin.setPassword(encryptedPassword);
        admin.setSalt(salt);
        admin.setRealName(realName);
        admin.setPhone(visitor.getPhone());
        admin.setEmail(visitor.getEmail());
        admin.setStatus(1);
        admin.setSource(1);
        admin.setSourceVisitorId(visitorId);
        admin.setPromoteTime(new Date());

        if(roleIdList != null && !roleIdList.isEmpty()){
            List<Long> longRoleIdList = new ArrayList<>();
            for(Number n : roleIdList){
                longRoleIdList.add(n.longValue());
            }
            admin.setRoleIdList(longRoleIdList);
        }

        sysAdminService.saveAdmin(admin);

        visitor.setPromotedToAdmin(1);
        visitor.setAdminId(admin.getAdminId());
        visitor.setUserType("ADMIN");
        visitor.setUpdateTime(new Date());
        this.updateById(visitor);
    }

    private void checkSensitiveWords(String username) {
        String lowerUsername = username.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lowerUsername.contains(word)) {
                throw new RRException("用户名包含敏感词，请更换");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementNoShowCount(Long visitorId) {
        VisitorEntity visitor = this.getById(visitorId);
        if (visitor == null) {
            throw new RRException("访客不存在");
        }

        Integer currentCount = visitor.getNoShowCount();
        if (currentCount == null) {
            currentCount = 0;
        }
        visitor.setNoShowCount(currentCount + 1);
        visitor.setUpdateTime(new Date());
        this.updateById(visitor);

        NoShowRuleDTO rule = ersConfigService.getNoShowRule();
        if (rule.getMaxNoShowCount() != null && visitor.getNoShowCount() >= rule.getMaxNoShowCount()) {
            this.addToBlacklist(visitorId, "爽约次数超过" + rule.getMaxNoShowCount() + "次，自动加入黑名单");
        }
    }
}
