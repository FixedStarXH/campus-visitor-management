package io.renren.modules.ers.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 入校申请表
 */
@Data
@TableName("ers_entry_application")
public class ErsEntryApplicationEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long applicationId;
    private String applicationNo;
    private Long visitorId;
    private String visitorName;
    private String phone;
    private String idCard;
    private String entryDate; // 预约入校日期，格式：yyyy-MM-dd
    private Long slotId;
    private String timeSlot; // 时间段名称
    private Date entryStartTime;
    private Date entryEndTime;
    private String reason;
    private String visitUnit;
    private Integer companionCount;
    private String attachmentUrl;
    private Integer status; // 状态 0待审批 1已通过 2已拒绝 3已取消 4已爽约 5已完成
    private Long approvalUserId;
    private Date approvalTime;
    private String approvalRemark;
    private Date cancelTime;
    private String cancelReason;
    private String recordNo;
    private String qrCodeContent;
    private Integer deleted;
    private Date createTime;
    private Date updateTime;
}