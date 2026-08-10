package io.renren.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("ers_admin_role")
public class SysAdminRoleEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId
    private Long id;

    private Long adminId;

    private Long roleId;
}