package io.renren.modules.ers.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 特殊日期配置表
 */
@Data
@TableName("ers_special_date")
public class ErsSpecialDateEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long specialDateId;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private Integer dateType;
    private String remark;
    private Integer status;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
}