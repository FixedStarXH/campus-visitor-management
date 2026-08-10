package io.renren.modules.ers.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.ers.entity.EntryRecordEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EntryRecordDao extends BaseMapper<EntryRecordEntity> {
}