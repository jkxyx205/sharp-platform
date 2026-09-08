package com.rick.gateway.controller;

import com.rick.gateway.captcha.CaptchaJson;
import com.rick.gateway.captcha.CodeKind;
import com.rick.gateway.captcha.ValidateCodeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("sms/{mobile}")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SmsController {

    ValidateCodeService validateCodeService;

    /**
     * 注册用户
     * @param mobile
     */
    @GetMapping("register")
    public Mono<ResponseEntity<String>> register(@PathVariable String mobile,
                                                 @RequestHeader("deviceId") String deviceId) {
        return send(mobile, deviceId, "register");
    }

    /**
     * 手机登录验证码
     * @param mobile
     */
    @GetMapping("mobile_login")
    public Mono<ResponseEntity<String>> login(@PathVariable String mobile,
                                                 @RequestHeader("deviceId") String deviceId) {
        return send(mobile, deviceId, "mobile_login");
    }

    /**
     * 按业务类型发送短信验证码（type 须在 captcha.types 配置为 kind=sms），
     * 发送成功后存入内存 mobile:deviceId:type，等待业务 URL 过滤器校验。
     *
     * @param mobile
     * @param deviceId
     * @param type 业务类型，如 register、chgpwd
     */
    @GetMapping
    public Mono<ResponseEntity<String>> send(@PathVariable String mobile,
                                             @RequestHeader("deviceId") String deviceId,
                                             @RequestParam String type) {
        // 阻塞发送放到 boundedElastic，勿占事件循环
        return Mono.fromCallable(() -> {
                    if (validateCodeService.typeSpec(type).getKind() != CodeKind.SMS) {
                        throw new IllegalArgumentException("type 不是短信验证码: " + type);
                    }
                    validateCodeService.sendCode(type, mobile, deviceId);
                    return ResponseEntity.ok().<String>build();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(IllegalArgumentException.class, e -> Mono.just(json(400, e.getMessage())))
                .onErrorResume(e -> Mono.just(json(500, "验证码发送失败")));
    }

    private static ResponseEntity<String> json(int status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CaptchaJson.error(status, message));
    }
}
