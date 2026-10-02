package com.rick.gateway.controller;

import com.rick.gateway.captcha.CaptchaJson;
import com.rick.gateway.captcha.ClientIpResolver;
import com.rick.gateway.captcha.CodeKind;
import com.rick.gateway.captcha.PreHandlerException;
import com.rick.gateway.captcha.RateLimitException;
import com.rick.gateway.captcha.ValidateCodeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
                                                 @RequestHeader("deviceId") String deviceId,
                                                 ServerHttpRequest request) {
        return send(mobile, deviceId, "register", request);
    }

    /**
     * 手机登录验证码
     * @param mobile
     */
    @GetMapping("mobile_login")
    public Mono<ResponseEntity<String>> login(@PathVariable String mobile,
                                              @RequestHeader("deviceId") String deviceId,
                                              ServerHttpRequest request) {
        return send(mobile, deviceId, "mobile_login", request);
    }

    /**
     * 按业务类型发送短信验证码（type 须在 captcha.types 配置为 kind=sms），
     * 发送成功后存入 mobile:deviceId:type，等待业务 URL 过滤器校验。
     *
     * @param mobile
     * @param deviceId
     * @param type    业务类型，如 register、chgpwd
     */
    @GetMapping
    public Mono<ResponseEntity<String>> send(@PathVariable String mobile,
                                             @RequestHeader("deviceId") String deviceId,
                                             @RequestParam String type,
                                             ServerHttpRequest request) {
        // 类型校验同步先行（便宜），发送链路在 ValidateCodeService 内自行调度 boundedElastic
        if (validateCodeService.typeSpec(type).getKind() != CodeKind.SMS) {
            return Mono.just(json(400, "type 不是短信验证码: " + type));
        }
        String ip = ClientIpResolver.resolve(request);
        return validateCodeService.sendCode(type, mobile, deviceId, ip)
                .map(v -> ResponseEntity.ok().<String>build())
                // 业务预检查拒绝（如「该手机号已注册」）：文案直达前端
                .onErrorResume(PreHandlerException.class, e -> Mono.just(json(400, e.getMessage())))
                // 频率超限
                .onErrorResume(RateLimitException.class, e -> Mono.just(json(429, e.getMessage())))
                .onErrorResume(e -> Mono.just(json(500, "验证码发送失败")));
    }

    private static ResponseEntity<String> json(int status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(CaptchaJson.error(status, message));
    }
}
