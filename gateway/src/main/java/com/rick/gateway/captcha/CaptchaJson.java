package com.rick.gateway.captcha;

/**
 * 统一 JSON 错误响应体，与 security 链 401 格式一致：{"code":...,"message":"..."}。
 */
public final class CaptchaJson {

    private CaptchaJson() {
    }

    public static String error(int code, String message) {
        String escaped = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"code\":" + code + ",\"message\":\"" + escaped + "\"}";
    }
}
