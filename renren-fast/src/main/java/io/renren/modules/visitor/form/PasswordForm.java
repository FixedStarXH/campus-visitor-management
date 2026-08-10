package io.renren.modules.visitor.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import javax.validation.constraints.Pattern;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@ApiModel("修改密码表单")
public class PasswordForm implements Serializable {

    @NotBlank(message = "旧密码不能为空")
    @ApiModelProperty("旧密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Length(min = 8, message = "新密码长度不能少于8位")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d!@#$%^&*.]{8,}$",
            message = "新密码需包含字母和数字")
    @ApiModelProperty("新密码")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @ApiModelProperty("确认密码")
    private String confirmPassword;
}