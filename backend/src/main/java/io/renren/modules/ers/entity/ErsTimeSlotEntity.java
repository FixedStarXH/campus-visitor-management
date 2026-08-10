package io.renren.modules.ers.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 入校时间段配置表
 */
@Data
@TableName("ers_time_slot")
public class ErsTimeSlotEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long slotId;
    private String slotName;
    private String startTime; // 时间字符串，格式：HH:mm:ss
    private String endTime; // 时间字符串，格式：HH:mm:ss
    private Integer maxCount;
    private Integer currentCount;
    private Integer status;
    private Integer sort;
    private String remark;
    private Long createBy;
    private Date createTime;
    private Long updateBy;
    private Date updateTime;
}
