package io.renren.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.common.utils.PageUtils;
import io.renren.modules.sys.entity.SysAdminEntity;
import java.util.Map;

public interface SysAdminService extends IService<SysAdminEntity> {
    PageUtils queryPage(Map<String, Object> params);
    void saveAdmin(SysAdminEntity admin);
    void update(SysAdminEntity admin);
    void deleteBatch(Long[] adminIds);
    Map<String, Object> checkPromoteCondition(Long userId);
    void promoteToAdmin(Long visitorId, SysAdminEntity admin);
    SysAdminEntity queryByUserName(String username);
    SysAdminEntity queryByPhone(String phone);
    SysAdminEntity queryByEmail(String email);
    SysAdminEntity queryByPhoneExcludeId(String phone, Long excludeId);
    SysAdminEntity queryByEmailExcludeId(String email, Long excludeId);
    boolean updatePassword(Long adminId, String password, String newPassword);
}