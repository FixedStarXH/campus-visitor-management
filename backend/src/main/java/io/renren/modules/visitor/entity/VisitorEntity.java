package io.renren.modules.visitor.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ers_visitor")
public class VisitorEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    private String account;

    private String username;

    private String password;

    private String salt;

    private String realName;

    private Integer gender;

    private String phone;

    private String email;

    private String avatar;

    private String userType;

    @com.baomidou.mybatisplus.annotation.TableField("auth_status")
    private Integer authStatus;

    private Integer status;

    private Integer blacklistStatus;

    private Integer promotedToAdmin;

    private Integer noShowCount;

    private Date lastLoginTime;

    private String lastLoginIp;

    private String remark;

    private Integer deleted;

    private Long createBy;

    private Date createTime;

    private Long updateBy;

    private Date updateTime;

    private Date blacklistEndTime;

    private String blacklistReason;

    private Long adminId;
}
