package io.renren.modules.application.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("提交入校申请表单")
public class ApplicationForm implements Serializable {

    @Length(max = 100, message = "到访单位长度不能超过100")
    @ApiModelProperty("到访单位/部门")
    private String visitUnit;

    @NotBlank(message = "访客姓名不能为空")
    @Length(max = 50, message = "访客姓名长度不能超过50")
    @ApiModelProperty("访客姓名")
    private String visitorName;

    @NotBlank(message = "访客手机不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty("访客手机号")
    private String phone;

    @NotNull(message = "预约入校日期不能为空")
    @FutureOrPresent(message = "预约日期不能是过去时间")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty("预约入校日期")
    private Date entryDate;

    @NotNull(message = "预约开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("预约开始时间")
    private Date entryStartTime;

    @NotNull(message = "预约结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty("预约结束时间")
    private Date entryEndTime;

    @NotBlank(message = "入校事由不能为空")
    @Length(max = 500, message = "入校事由长度不能超过500")
    @ApiModelProperty("入校事由")
    private String reason;

    @NotNull(message = "陪同人数不能为空")
    @Min(value = 0, message = "陪同人数最小为0")
    @Max(value = 100, message = "陪同人数最多为100")
    @ApiModelProperty("陪同人数")
    private Integer companionCount;

    @Pattern(regexp = "^([京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-HJ-NP-Z0-9]{5})?$",
            message = "车牌号格式不正确，如：京A12345")
    @ApiModelProperty("车牌号（选填）")
    private String vehiclePlate;
}