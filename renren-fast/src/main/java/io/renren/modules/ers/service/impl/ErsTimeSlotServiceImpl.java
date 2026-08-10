package io.renren.modules.ers.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.modules.ers.dao.ErsTimeSlotDao;
import io.renren.modules.ers.entity.ErsTimeSlotEntity;
import io.renren.modules.ers.service.ErsTimeSlotService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 入校时间段配置表 Service 实现类
 */
@Service("ersTimeSlotService")
public class ErsTimeSlotServiceImpl extends ServiceImpl<ErsTimeSlotDao, ErsTimeSlotEntity> implements ErsTimeSlotService {

    @Override
    public List<ErsTimeSlotEntity> getActiveSlots() {
        return this.list(new QueryWrapper<ErsTimeSlotEntity>()
                .eq("status", 1)
                .orderByAsc("sort"));
    }
}
