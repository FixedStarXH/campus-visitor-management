package io.renren.modules.ers.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.common.utils.PageUtils;
import io.renren.modules.ers.entity.ErsUserEntity;

import java.util.Date;
import java.util.Map;

/**
 * 用户服务接口
 * 
 * @author ERS System
 */
public interface ErsUserService extends IService<ErsUserEntity> {

    /**
     * 分页查询用户列表
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 保存用户
     */
    void saveUser(ErsUserEntity user, Long createUserId);

    /**
     * 更新用户
     */
    void updateUser(ErsUserEntity user);

    /**
     * 批量删除用户
     */
    void deleteBatch(Long[] userIds);

    /**
     * 更新用户状态
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 加入黑名单
     */
    void addToBlacklist(Long userId, String reason, Date expireTime);

    /**
     * 移出黑名单
     */
    void removeFromBlacklist(Long userId);

    /**
     * 检查用户是否可提升为管理员
     */
    boolean checkPromotionEligibility(Long userId);
}