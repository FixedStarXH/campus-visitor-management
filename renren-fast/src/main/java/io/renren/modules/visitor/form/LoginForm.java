package io.renren.modules.visitor.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@ApiModel("登录表单")
public class LoginForm implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @ApiModelProperty("用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @ApiModelProperty("密码")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @ApiModelProperty("验证码")
    private String captcha;

    @NotBlank(message = "uuid不能为空")
    @ApiModelProperty("验证码uuid")
    private String uuid;
}