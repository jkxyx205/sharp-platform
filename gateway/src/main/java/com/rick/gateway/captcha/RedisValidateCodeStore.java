package com.rick.gateway.captcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 验证码 Redis 存储：多实例共享、重启不丢失。
 * <p>
 * key 自带 TTL，过期由 Redis 自动清理；get 仍做 {@link ValidateCode#isExpired()} 兜底删除，
 * 避免 TTL 秒级取整导致的过期残留；consume 用 Lua 原子比对 content+expireAt 后删除，
 * 防止并发双提交与重发误删新码。
 * <p>
 * 使用 {@link ReactiveStringRedisTemplate} 非阻塞响应式 IO，避免阻塞 Netty 事件循环。
 */
@Component
@ConditionalOnProperty(name = "captcha.store", havingValue = "redis")
@ConditionalOnClass(ReactiveStringRedisTemplate.class)
public class RedisValidateCodeStore implements ValidateCodeStore {

    private static final Logger log = LoggerFactory.getLogger(RedisValidateCodeStore.class);

    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_EXPIRE_AT = "expireAt";

    /** KEYS[1]=key ARGV[1]=期望 content ARGV[2]=期望 expireAt；两字段都相等才删除 */
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('HGET', KEYS[1], 'content') == ARGV[1] " +
                    "and redis.call('HGET', KEYS[1], 'expireAt') == ARGV[2] " +
                    "then return redis.call('DEL', KEYS[1]) else return 0 end",
                    Long.class);

    private final ReactiveStringRedisTemplate redis;

    public RedisValidateCodeStore(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> put(String key, ValidateCode code) {
        long ttl = Duration.between(Instant.now(), code.expireAt()).getSeconds();
        if (ttl <= 0) {
            // 已过期不写入（等同内存实现惰性清理后不可见）
            return Mono.empty();
        }
        Map<String, String> fields = Map.of(
                FIELD_CONTENT, code.content(),
                FIELD_EXPIRE_AT, code.expireAt().toString());
        return redis.opsForHash().putAll(key, fields)
                .then(redis.expire(key, Duration.ofSeconds(ttl)))
                .then();
    }

    @Override
    public Mono<ValidateCode> get(String key) {
        return redis.opsForHash().multiGet(key, List.of(FIELD_CONTENT, FIELD_EXPIRE_AT))
                .flatMap(values -> {
                    if (values == null || values.size() < 2) {
                        return Mono.<ValidateCode>empty();
                    }
                    String content = stringOf(values.get(0));
                    String expireAt = stringOf(values.get(1));
                    if (content == null || expireAt == null) {
                        return Mono.<ValidateCode>empty();
                    }
                    try {
                        ValidateCode code = new ValidateCode(content, Instant.parse(expireAt));
                        if (code.isExpired()) {
                            return redis.delete(key).then(Mono.<ValidateCode>empty());
                        }
                        return Mono.just(code);
                    } catch (RuntimeException e) {
                        log.warn("验证码反序列化失败, key={}: {}", key, e.toString());
                        return redis.delete(key).then(Mono.<ValidateCode>empty());
                    }
                });
    }

    private static String stringOf(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public Mono<Boolean> consume(String key, ValidateCode code) {
        return redis.execute(CONSUME_SCRIPT, Collections.singletonList(key),
                        code.content(), code.expireAt().toString())
                .next()
                .map(removed -> removed != null && removed > 0)
                .defaultIfEmpty(false);
    }

    @Override
    public Mono<Void> remove(String key) {
        return redis.delete(key).then();
    }
}
