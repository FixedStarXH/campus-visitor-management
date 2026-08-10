package io.renren.modules.ers.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.ers.entity.ErsUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口
 * 
 * @author ERS System
 */
@Mapper
public interface ErsUserDao extends BaseMapper<ErsUserEntity> {

}