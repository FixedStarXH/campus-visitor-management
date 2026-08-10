package io.renren.modules.ers.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.ers.entity.ErsSpecialDateEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 特殊日期配置表 Dao
 */
@Mapper
public interface ErsSpecialDateDao extends BaseMapper<ErsSpecialDateEntity> {
}
