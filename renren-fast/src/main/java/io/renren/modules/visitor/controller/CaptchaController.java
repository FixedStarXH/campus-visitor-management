package io.renren.modules.visitor.controller;

import io.renren.common.utils.R;
import io.renren.modules.sys.service.SysCaptchaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/captcha")
@Api("验证码接口")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CaptchaController {

    // 1. 添加日志（关键！排查错误用）
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SysCaptchaService sysCaptchaService;

    @GetMapping
    @ApiOperation("获取验证码")
    public R captcha() {
        String uuid = UUID.randomUUID().toString();
        BufferedImage image = sysCaptchaService.getCaptcha(uuid);

        // 2. try-with-resources 自动关闭流
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);

            // 3. 简化代码，直接链式调用
            return R.ok().put("uuid", uuid).put("captchaImage", base64);
        } catch (Exception e) {
            // 打印错误日志
            logger.error("获取验证码失败", e);
            return R.error("获取验证码失败");
        }
    }
}