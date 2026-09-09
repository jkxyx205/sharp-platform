package com.rick.gateway.captcha;

/**
 * 预检查业务拒绝：message 面向前端（如「该手机号已注册」），由 SmsController 映射为 400 JSON。
 * 纯控制流，不需要堆栈。
 */
public class PreHandlerException extends RuntimeException {

    public PreHandlerException(String message) {
        super(message, null, false, false);
    }
}
