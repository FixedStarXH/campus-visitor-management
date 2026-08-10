package io.renren.modules.ers.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.ers.entity.ErsTimeSlotEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入校时间段配置表 Dao
 */
@Mapper
public interface ErsTimeSlotDao extends BaseMapper<ErsTimeSlotEntity> {
}
