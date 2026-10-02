package com.rick.gateway.captcha;

import reactor.core.publisher.Mono;

/**
 * 验证码存储抽象：发送时写入，业务过滤器校验。
 * key：短信为 mobile:deviceId:type，图片为 deviceId:type。
 * <p>
 * 两种实现按 {@code captcha.store} 选择：默认内存 {@link InMemoryValidateCodeStore}（缺省），
 * 多实例共享用 {@link RedisValidateCodeStore}（设 {@code captcha.store=redis}）。
 * <p>
 * 方法返回 Mono：Redis 实现为非阻塞响应式 IO，避免阻塞 Netty 事件循环。
 */
public interface ValidateCodeStore {

    Mono<Void> put(String key, ValidateCode code);

    /**
     * 读取不删除；过期条目惰性清理。
     */
    Mono<ValidateCode> get(String key);

    /**
     * 原子消费：目标仍存在时才删除（并发双提交只允许一个通过）。
     */
    Mono<Boolean> consume(String key, ValidateCode code);

    Mono<Void> remove(String key);

    static String buildKey(CodeKind kind, String mobile, String deviceId, String type) {
        return kind == CodeKind.IMAGE
                ? deviceId + ":" + type
                : mobile + ":" + deviceId + ":" + type;
    }
}
