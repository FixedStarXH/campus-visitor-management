package io.renren.modules.visitor.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Data
@ApiModel("修改个人信息表单")
public class UpdateForm implements Serializable {

    @Length(max = 50, message = "真实姓名长度不能超过50")
    @ApiModelProperty("真实姓名")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty("手机号")
    private String mobile;

    @Email(message = "邮箱格式不正确")
    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("头像URL")
    private String avatar;

    @Range(min = 0, max = 1, message = "性别值不正确 0-女 1-男")
    @ApiModelProperty("性别 0-女 1-男")
    private Integer gender;
}