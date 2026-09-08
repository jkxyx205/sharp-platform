package com.rick.platform.module.user.controller;

import com.rick.platform.module.user.entity.User;
import com.rick.platform.module.user.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class UserController {

    UserService userService;

    SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 当前登录用户：网关认证后透传身份，由用户上下文注入。
     */
    @GetMapping("me")
    public User me(User user) {
        return user;
    }

    @PutMapping("chgpwd")
    public void chgpwd(User user, String password) {
        // TODO
//        userService.changePassword()
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(user.getId()), "/queue/message", "密码修改成功");
    }

    /**
     * 推送消息到前端
     * @param user
     */
    @GetMapping("messages")
    public void getMessage(User user) {
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(user.getId()), "/queue/message", "message :" + System.currentTimeMillis());
    }
}
