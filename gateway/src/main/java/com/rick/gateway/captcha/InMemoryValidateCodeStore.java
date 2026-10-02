package com.rick.gateway.captcha;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码内存存储：单机默认实现，进程重启即丢失。
 * <p>
 * 语义不变：get 惰性清理过期码；consume 原子比对删除（{@code ConcurrentHashMap.remove(key, value)}）。
 * 方法返回 Mono 以与 Redis 实现统一接口；内存操作本身非阻塞。
 */
@Component
@ConditionalOnProperty(name = "captcha.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryValidateCodeStore implements ValidateCodeStore {

    private final Map<String, ValidateCode> codes = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> put(String key, ValidateCode code) {
        return Mono.fromRunnable(() -> codes.put(key, code)).then();
    }

    @Override
    public Mono<ValidateCode> get(String key) {
        return Mono.defer(() -> {
            ValidateCode code = codes.get(key);
            if (code == null) {
                return Mono.empty();
            }
            if (code.isExpired()) {
                codes.remove(key, code);
                return Mono.empty();
            }
            return Mono.just(code);
        });
    }

    @Override
    public Mono<Boolean> consume(String key, ValidateCode code) {
        return Mono.just(codes.remove(key, code));
    }

    @Override
    public Mono<Void> remove(String key) {
        return Mono.fromRunnable(() -> codes.remove(key)).then();
    }
}
