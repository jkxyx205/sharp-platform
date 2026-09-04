package com.rick.platform.module.user.controller;

import com.rick.platform.module.user.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    /**
     * 当前登录用户：网关认证后透传身份，由用户上下文注入。
     */
    @GetMapping("me")
    public User me(User user) {
        return user;
    }
}
