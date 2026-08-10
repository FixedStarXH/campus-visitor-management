package io.renren.modules.ers.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.modules.ers.entity.ErsConfigEntity;
import io.renren.modules.ers.dto.NoShowRuleDTO;

/**
 * 入校登记系统业务配置表 Service
 */
public interface ErsConfigService extends IService<ErsConfigEntity> {
    /** 获取爽约规则 */
    NoShowRuleDTO getNoShowRule();
    /** 更新爽约规则 */
    void updateNoShowRule(NoShowRuleDTO dto);
}
