package io.renren.modules.ers.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.modules.ers.entity.ErsSpecialDateEntity;

/**
 * 特殊日期配置表 Service
 */
public interface ErsSpecialDateService extends IService<ErsSpecialDateEntity> {
    /** 检查日期是否允许入校 */
    boolean checkDateAvailable(String date);
}
