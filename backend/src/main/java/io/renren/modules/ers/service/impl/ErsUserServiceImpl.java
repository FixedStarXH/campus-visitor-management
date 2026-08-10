package io.renren.modules.ers.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.common.utils.PageUtils;
import io.renren.common.utils.Query;

import io.renren.modules.ers.dao.ErsUserDao;
import io.renren.modules.ers.entity.ErsUserEntity;
import io.renren.modules.ers.service.ErsUserService;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.crypto.hash.Sha256Hash;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

/**
 * 用户服务实现类
 * 
 * @author ERS System
 */
@Service("ersUserService")
public class ErsUserServiceImpl extends ServiceImpl<ErsUserDao, ErsUserEntity> implements ErsUserService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String username = (String) params.get("username");
        String mobile = (String) params.get("mobile");
        String status = (String) params.get("status");

        IPage<ErsUserEntity> page = this.page(
                new Query<ErsUserEntity>().getPage(params),
                new QueryWrapper<ErsUserEntity>()
                        .like(StringUtils.isNotBlank(username), "username", username)
                        .eq(StringUtils.isNotBlank(mobile), "mobile", mobile)
                        .eq(StringUtils.isNotBlank(status), "status", status)
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUser(ErsUserEntity user, Long createUserId) {
        String salt = RandomStringUtils.randomAlphanumeric(20);
        user.setSalt(salt);

        if (StringUtils.isBlank(user.getPassword())) {
            user.setPassword("123456");
        }

        user.setPassword(new Sha256Hash(user.getPassword(), user.getSalt()).toHex());

        user.setStatus(1);
        user.setBlacklistFlag(0);
        user.setIsPromoted(0);
        user.setCreateTime(new Date());
        user.setCreateUserId(createUserId);

        this.save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(ErsUserEntity user) {
        if (StringUtils.isBlank(user.getPassword())) {
            user.setPassword(null);
        } else {
            // 密码不为空时重新加密
            ErsUserEntity userEntity = this.getById(user.getUserId());
            user.setPassword(new Sha256Hash(user.getPassword(), userEntity.getSalt()).toHex());
        }
        
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Long[] userIds) {
        this.removeByIds(java.util.Arrays.asList(userIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Integer status) {
        ErsUserEntity user = new ErsUserEntity();
        user.setUserId(userId);
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addToBlacklist(Long userId, String reason, Date expireTime) {
        ErsUserEntity user = new ErsUserEntity();
        user.setUserId(userId);
        user.setBlacklistFlag(1);
        user.setBlacklistReason(reason);
        user.setBlacklistExpireTime(expireTime);
        user.setStatus(0); // 加入黑名单时自动禁用
        this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
  public void removeFromBlacklist(Long userId) {
        ErsUserEntity user = new ErsUserEntity();
        user.setUserId(userId);
        user.setBlacklistFlag(0);
        user.setBlacklistReason(null);
        user.setBlacklistExpireTime(null);
        user.setStatus(1); // 移出黑名单时自动启用
        this.updateById(user);
    }

    @Override
    public boolean checkPromotionEligibility(Long userId) {
        ErsUserEntity user = this.getById(userId);
        if (user == null || user.getBlacklistFlag() == 1 || user.getStatus() == 0) {
            return false;
        }
        
        // 这里可以添加更多的提升条件检查逻辑
        // 例如：履约次数、信誉评分、来访频次等
        
        return true;
    }
}