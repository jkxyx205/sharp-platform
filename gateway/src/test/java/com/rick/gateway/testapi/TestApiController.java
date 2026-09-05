package com.rick.gateway.testapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅测试用：验证码校验通过后的下游端点（非网关路由，直接命中控制器）。
 */
@RestController
@RequestMapping("test")
public class TestApiController {

    @GetMapping({"image-flow", "sms-flow", "protected", "expired-flow"})
    public String ok() {
        return "ok";
    }
}
