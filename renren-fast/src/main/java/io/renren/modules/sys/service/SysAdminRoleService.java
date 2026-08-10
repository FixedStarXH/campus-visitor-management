package io.renren.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import io.renren.modules.sys.entity.SysAdminRoleEntity;
import java.util.List;

public interface SysAdminRoleService extends IService<SysAdminRoleEntity> {
    void saveOrUpdate(Long adminId, List<Long> roleIdList);
    List<Long> queryRoleIdList(Long adminId);
}