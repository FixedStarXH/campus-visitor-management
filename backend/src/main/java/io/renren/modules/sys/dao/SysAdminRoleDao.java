package io.renren.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.renren.modules.sys.entity.SysAdminRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface SysAdminRoleDao extends BaseMapper<SysAdminRoleEntity> {
    List<Long> queryRoleIdList(Long adminId);
}