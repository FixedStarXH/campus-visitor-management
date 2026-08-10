package io.renren.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.renren.common.utils.MapUtils;
import io.renren.modules.sys.dao.SysAdminRoleDao;
import io.renren.modules.sys.entity.SysAdminRoleEntity;
import io.renren.modules.sys.service.SysAdminRoleService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service("sysAdminRoleService")
public class SysAdminRoleServiceImpl extends ServiceImpl<SysAdminRoleDao, SysAdminRoleEntity> implements SysAdminRoleService {

    @Override
    public void saveOrUpdate(Long adminId, List<Long> roleIdList) {
        this.removeByMap(new MapUtils().put("admin_id", adminId));

        if(roleIdList == null || roleIdList.size() == 0){
            return ;
        }

        for(Long roleId : roleIdList){
            SysAdminRoleEntity entity = new SysAdminRoleEntity();
            entity.setAdminId(adminId);
            entity.setRoleId(roleId);
            this.save(entity);
        }
    }

    @Override
    public List<Long> queryRoleIdList(Long adminId) {
        return baseMapper.queryRoleIdList(adminId);
    }
}