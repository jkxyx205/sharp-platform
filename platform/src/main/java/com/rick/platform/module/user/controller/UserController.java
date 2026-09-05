package com.rick.platform.module.user.controller;

import com.rick.platform.module.user.entity.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    /**
     * 当前登录用户：网关认证后透传身份，由用户上下文注入。
     */
    @GetMapping("me")
    public User me(User user) {
        return user;
    }

    @PutMapping("chgpwd")
    public void chgpwd(User user, @RequestHeader("deviceId") String deviceId) {
        // 存入 redis mobile:deviceId:type
        // TODO 抽取短信验证的模块
        // TODO 添加过滤 /users/chgpwd 进行拦截。获取 deviceId， 业务类型 chgpwd mobile：默认当前用户。扩展方法获取 mobile 的方法
    }
}
