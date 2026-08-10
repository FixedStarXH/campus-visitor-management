package io.renren.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.renren.common.validator.group.AddGroup;
import io.renren.common.validator.group.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@TableName("ers_admin")
public class SysAdminEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long adminId;

    @NotBlank(message="用户名不能为空", groups = {AddGroup.class, UpdateGroup.class})
    @Length(min = 3, max = 20, message = "用户名长度需在3-20位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线", groups = {AddGroup.class, UpdateGroup.class})
    private String username;

    @NotBlank(message="密码不能为空", groups = AddGroup.class)
    @Length(min = 6, message = "密码长度不能少于6位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d!@#$%^&*.]{6,}$", message = "密码需包含字母和数字", groups = AddGroup.class)
    private String password;

    private String salt;

    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确", groups = {AddGroup.class, UpdateGroup.class})
    private String phone;

    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确", groups = {AddGroup.class, UpdateGroup.class})
    private String email;

    @Range(min = 0, max = 1, message = "状态值只能为0或1")
    private Integer status;

    private Integer source;

    private Long sourceVisitorId;

    private Date promoteTime;

    @TableField(exist=false)
    private List<Long> roleIdList;

    private Long createUserId;

    private Date createTime;

    private Integer deleted;
}
