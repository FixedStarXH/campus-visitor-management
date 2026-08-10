package io.renren.modules.visitor.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.visitor.entity.VisitorEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VisitorDao extends BaseMapper<VisitorEntity> {
}