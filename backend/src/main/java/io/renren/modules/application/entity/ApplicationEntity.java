package io.renren.modules.application.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 入校申请实体类
 */
@Data
@TableName("ers_application")
public class ApplicationEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 申请ID
     */
    @TableId
    private Long id;

    /**
     * 访客ID
     */
    private Long visitorId;

    /**
     * 访客姓名
     */
    private String visitorName;

    /**
     * 访客手机号
     */
    private String phone;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 访问单位
     */
    private String visitUnit;

    /**
     * 车牌号
     */
    private String vehiclePlate;

    /**
     * 入校事由
     */
    private String reason;

    /**
     * 预约入校日期
     */
    private Date entryDate;

    /**
     * 预约入校开始时间
     */
    private Date entryStartTime;

    /**
     * 预约入校结束时间
     */
    private Date entryEndTime;

    /**
     * 时间段ID
     */
    private Long slotId;

    /**
     * 同行人数
     */
    private Integer companionCount;

    /**
     * 申请编号
     */
    private String applicationNo;

    /**
     * 申请状态：0-待审批 1-已通过 2-已拒绝 3-已取消 4-已预约 5-已完成
     */
    private Integer status;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 入校编号/通行码
     */
    private String entryCode;

    /**
     * 附件URL
     */
    private String attachmentUrl;

    /**
     * 审批人ID
     */
    private Long approvalUserId;

    /**
     * 审批时间
     */
    private Date approvalTime;

    /**
     * 审批意见/拒绝原因
     */
    private String approvalRemark;

    /**
     * 取消时间
     */
    private Date cancelTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 软删除标记：0-未删除 1-已删除
     */
    private Integer deleted = 0;
}
