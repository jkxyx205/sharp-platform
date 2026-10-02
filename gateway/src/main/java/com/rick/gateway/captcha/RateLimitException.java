package com.rick.gateway.captcha;

/**
 * 验证码发送频率超限：由 {@link CodeRateLimiter#check} 抛出，
 * 控制器映射为 429 响应。
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
