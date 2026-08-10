package io.renren.modules.application.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("申请列表查询表单")
public class ApplicationQueryForm implements Serializable {

    @ApiModelProperty("状态 0待审批 1已通过 2已拒绝 3已取消 4已爽约 5已完成")
    private Integer status;

    @ApiModelProperty("开始时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd")  // ← 用这个
    private Date startTime;

    @ApiModelProperty("结束时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd")  // ← 用这个
    private Date endTime;

    @Min(value = 1, message = "页码最小为1")
    @ApiModelProperty("页码，默认1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    @ApiModelProperty("每页条数，默认20")
    private Integer pageSize = 20;
}