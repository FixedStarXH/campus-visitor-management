package io.renren.modules.visitor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.modules.visitor.entity.VisitorEntity;
import io.renren.modules.visitor.form.LoginForm;
import io.renren.modules.visitor.form.VisitorRegisterForm;

import java.util.List;
import java.util.Map;

public interface VisitorService extends IService<VisitorEntity> {
    void register(VisitorRegisterForm form);
    Map<String, Object> login(LoginForm form);

    void addToBlacklist(Long visitorId, String reason);

    void removeFromBlacklist(Long visitorId);

    Map<String, Object> checkPromoteCondition(Long visitorId);

    void promoteToAdmin(Long visitorId, String username, String password, String realName, String phone);

    void promoteToAdminSimple(Long visitorId, String username, String realName, List<Long> roleIdList);

    void incrementNoShowCount(Long visitorId);
}