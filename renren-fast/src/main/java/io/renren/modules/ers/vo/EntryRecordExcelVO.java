package io.renren.modules.ers.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class EntryRecordExcelVO {

    @ExcelProperty("记录编号")
    private String recordNo;

    @ExcelProperty("访客姓名")
    private String visitorName;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("预约日期")
    private Date entryDate;

    @ExcelProperty("预约开始时间")
    private Date entryStartTime;

    @ExcelProperty("预约结束时间")
    private Date entryEndTime;

    @ExcelProperty("实际入校时间")
    private Date actualEntryTime;

    @ExcelProperty("核销状态")
    private String verifyStatus;

    @ExcelProperty("核销校门")
    private String verifyGate;

    @ExcelProperty("记录状态")
    private String recordStatus;

    @ExcelProperty("备注")
    private String remark;
}