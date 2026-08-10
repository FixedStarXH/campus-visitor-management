package io.renren.modules.ers.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.modules.ers.dao.ErsConfigDao;
import io.renren.modules.ers.dto.NoShowRuleDTO;
import io.renren.modules.ers.entity.ErsConfigEntity;
import io.renren.modules.ers.service.ErsConfigService;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 入校登记系统业务配置表 Service 实现类
 */
@Service("ersConfigService")
public class ErsConfigServiceImpl extends ServiceImpl<ErsConfigDao, ErsConfigEntity> implements ErsConfigService {

    private static final String NO_SHOW_RULE_KEY = "NO_SHOW_RULE";

    @Override
    public NoShowRuleDTO getNoShowRule() {
        ErsConfigEntity config = this.getOne(new QueryWrapper<ErsConfigEntity>().eq("config_key", NO_SHOW_RULE_KEY));
        if (config == null) {
            // 如果数据库没配置，返回一个默认值
            NoShowRuleDTO defaultRule = new NoShowRuleDTO();
            defaultRule.setLateMinutes(30);
            defaultRule.setMaxNoShowCount(3);
            defaultRule.setBlacklistDays(30);
            return defaultRule;
        }
        return JSON.parseObject(config.getConfigValue(), NoShowRuleDTO.class);
    }

    @Override
    public void updateNoShowRule(NoShowRuleDTO dto) {
        ErsConfigEntity config = this.getOne(new QueryWrapper<ErsConfigEntity>().eq("config_key", NO_SHOW_RULE_KEY));
        if (config == null) {
            config = new ErsConfigEntity();
            config.setConfigKey(NO_SHOW_RULE_KEY);
            config.setConfigName("爽约规则配置");
            config.setValueType("json");
            config.setCreateTime(new Date());
        }
        config.setConfigValue(JSON.toJSONString(dto));
        config.setUpdateTime(new Date());
        config.setStatus(1);
        this.saveOrUpdate(config);
    }
}
