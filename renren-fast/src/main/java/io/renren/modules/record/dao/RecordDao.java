package io.renren.modules.record.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.application.entity.ApplicationEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecordDao extends BaseMapper<ApplicationEntity> {
}