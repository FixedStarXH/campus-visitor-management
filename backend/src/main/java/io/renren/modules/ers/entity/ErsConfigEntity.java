package io.renren.modules.ers.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 入校登记系统业务配置表
 */
@Data
@TableName("ers_config")
public class ErsConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long configId;
    private String configKey;
    private String configName;
    private String configValue;
    private String valueType;
    private String remark;
    private Integer status;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
}
