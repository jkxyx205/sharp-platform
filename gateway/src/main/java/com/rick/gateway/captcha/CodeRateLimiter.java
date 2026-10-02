package com.rick.gateway.captcha;

import reactor.core.publisher.Mono;

/**
 * 验证码发送频率限制：发送前校验，超限抛 {@link RateLimitException}。
 * <p>
 * 维度：mobile（仅短信）、deviceId、客户端 IP。按 {@code captcha.store} 选择实现，
 * 默认内存 {@link InMemoryRateLimiter}，多实例共享用 {@link RedisRateLimiter}
 * （设 {@code captcha.store=redis}）。
 */
public interface CodeRateLimiter {

    /**
     * 校验是否允许发送，超限以 {@link RateLimitException} 信号化传播。
     *
     * @param spec     验证码类型定义（区分图片/短信，图片码 mobile 为 null 时跳过 mobile 维度）
     * @param mobile   手机号（图片码为 null）
     * @param deviceId 设备标识
     * @param ip       客户端 IP
     */
    Mono<Void> check(ValidateCodeProperties.TypeSpec spec, String mobile, String deviceId, String ip);
}
