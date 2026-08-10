package io.renren.modules.ers.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.ers.entity.ErsConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入校登记系统业务配置表 Dao
 */
@Mapper
public interface ErsConfigDao extends BaseMapper<ErsConfigEntity> {
}
