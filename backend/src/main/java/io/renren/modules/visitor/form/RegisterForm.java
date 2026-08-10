package io.renren.modules.visitor.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

@Data
@ApiModel("用户注册表单")
public class RegisterForm implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Length(min = 3, max = 20, message = "用户名长度需在3-20位之间")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字、下划线")
    @ApiModelProperty("用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Length(min = 8, message = "密码长度不能少于8位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d!@#$%^&*.]{8,}$",
            message = "密码需包含字母和数字")
    @ApiModelProperty("密码")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @ApiModelProperty("确认密码")
    private String confirmPassword;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty("手机号")
    private String mobile;

    @Pattern(regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
            message = "邮箱格式不正确")
    @ApiModelProperty("邮箱（可选）")
    private String email;

    @ApiModelProperty("真实姓名")
    private String realName;

    @Range(min = 0, max = 1, message = "性别值不正确")
    @ApiModelProperty("性别 0-女 1-男")
    private Integer gender;

    @NotBlank(message = "验证码不能为空")
    @ApiModelProperty("验证码")
    private String captcha;

    @NotBlank(message = "uuid不能为空")
    @ApiModelProperty("验证码uuid")
    private String uuid;
}