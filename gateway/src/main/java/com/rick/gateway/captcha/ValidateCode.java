package com.rick.gateway.captcha;

import java.time.Instant;

/**
 * 验证码：内容 + 过期时间。
 */
public record ValidateCode(String content, Instant expireAt) {

    public boolean isExpired() {
        return Instant.now().isAfter(expireAt);
    }

    /**
     * 与用户输入比对：图片验证码不区分大小写，短信验证码为数字不受影响。
     */
    public boolean matches(String input) {
        return input != null && content.equalsIgnoreCase(input);
    }
}
