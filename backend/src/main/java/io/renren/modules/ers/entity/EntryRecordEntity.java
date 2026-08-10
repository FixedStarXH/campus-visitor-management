package io.renren.modules.ers.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ers_entry_record")
public class EntryRecordEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long recordId;

    private String recordNo;

    private Long applicationId;

    private Long visitorId;

    private String visitorName;

    private String phone;

    private Date entryDate;

    private Date entryStartTime;

    private Date entryEndTime;

    private Date actualEntryTime;

    private Integer verifyStatus;

    private String verifyGate;

    private Long verifyUserId;

    private String qrCodeContent;

    private Integer recordStatus;

    private String remark;

    private Date createTime;

    private Date updateTime;
}