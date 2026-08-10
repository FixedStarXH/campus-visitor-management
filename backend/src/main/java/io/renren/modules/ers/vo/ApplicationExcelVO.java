package io.renren.modules.ers.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class ApplicationExcelVO {

    @ExcelProperty("申请编号")
    private String applicationNo;

    @ExcelProperty("访客姓名")
    private String visitorName;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("入校日期")
    private Date entryDate;

    @ExcelProperty("预约入校时间")
    private Date entryStartTime;

    @ExcelProperty("预约离校时间")
    private Date entryEndTime;

    @ExcelProperty("入校事由")
    private String reason;

    @ExcelProperty("审批状态")
    private String statusText;

    @ExcelProperty("审批时间")
    private Date approvalTime;

    @ExcelProperty("审批备注")
    private String approvalRemark;
}