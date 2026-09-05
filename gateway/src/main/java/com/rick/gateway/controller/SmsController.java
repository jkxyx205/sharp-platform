package com.rick.gateway.controller;

import com.rick.sms.core.Sender;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("sms/{mobile}")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class SmsController {

    Sender sender;

    /**
     * 注册用户
     * @param mobile
     */
    @GetMapping("register")
    public void register(@PathVariable String mobile, @RequestHeader("deviceId") String deviceId) {
        send(mobile, deviceId, "register");
    }

    /**
     *
     * @param mobile
     * @param deviceId
     * @param type 业务类型
     *             - 注册
     */
    @GetMapping
    public void send(@PathVariable String mobile, @RequestHeader("deviceId") String deviceId, @RequestParam String type) {
        // 存入 redis mobile:deviceId:type
        // 等待业务验证

        if ("register".equals(type)) {
            Map<String, String> params = new HashMap<>(1);
            params.put("code", "123456");
            sender.send(mobile, "xx科技", "SMS_23320004", params);
        } else if ("chgpwd".equals(type)) {
            Map<String, String> params = new HashMap<>(1);
            params.put("code", "654321");
            sender.send(mobile, "xx科技", "SMS_3461330", params);
        }
    }
}
