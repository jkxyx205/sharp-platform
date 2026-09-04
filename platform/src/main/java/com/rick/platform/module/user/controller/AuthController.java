package com.rick.platform.module.user.controller;

import com.rick.platform.module.user.entity.User;
import com.rick.platform.module.user.exception.AuthException;
import com.rick.platform.module.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户注册/登录。网关通过 lb://platform 调用本接口校验凭证。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    public record RegisterRequest(@NotBlank String nickname,
                                  @NotBlank String mobile,
                                  @NotBlank String password,
                                  Character sex) {
    }

    public record LoginRequest(@NotBlank String mobile,
                               @NotBlank String password) {
    }

    /** 注册/登录成功返回的最小用户信息（不含密码） */
    public record UserInfo(Long id, String nickname, String mobile, String avatar) {
    }

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request.nickname(), request.mobile(),
                    request.password(), request.sex());
            return ResponseEntity.ok(toUserInfo(user));
        } catch (AuthException e) {
            return error(e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.mobile(), request.password());
            return ResponseEntity.ok(toUserInfo(user));
        } catch (AuthException e) {
            return error(e);
        }
    }

    private static UserInfo toUserInfo(User user) {
        return new UserInfo(user.getId(), user.getNickname(), user.getMobile(), user.getAvatar());
    }

    private static ResponseEntity<Map<String, String>> error(AuthException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of(
                "code", String.valueOf(e.getStatus().value()),
                "message", e.getMessage()));
    }
}
