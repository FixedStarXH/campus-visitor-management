package io.renren.modules.ers.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.modules.ers.entity.ErsTimeSlotEntity;
import java.util.List;

/**
 * 入校时间段配置表 Service
 */
public interface ErsTimeSlotService extends IService<ErsTimeSlotEntity> {
    /** 获取所有启用的时段 */
    List<ErsTimeSlotEntity> getActiveSlots();
}
