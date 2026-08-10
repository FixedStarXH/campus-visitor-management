package io.renren.modules.ers.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.ers.entity.ErsEntryApplicationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入校申请表 Dao
 */
@Mapper
public interface ErsEntryApplicationDao extends BaseMapper<ErsEntryApplicationEntity> {
}
