package io.renren.modules.sys.controller;

import io.renren.common.utils.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints used for local startup verification.
 */
@RestController
public class HomeController {

    @GetMapping("/")
    public R index() {
        return R.ok().put("msg", "renren-fast backend is running");
    }

    @GetMapping("/health")
    public R health() {
        return R.ok().put("status", "UP");
    }
}
