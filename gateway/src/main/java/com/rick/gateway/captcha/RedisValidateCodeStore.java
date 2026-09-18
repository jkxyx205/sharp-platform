package com.rick.gateway.captcha;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * 验证码 Redis 存储：多实例共享、重启不丢失。
 * <p>
 * key 自带 TTL，过期由 Redis 自动清理；get 仍做 {@link ValidateCode#isExpired()} 兜底删除，
 * 避免 TTL 秒级取整导致的过期残留；consume 用 Lua 原子比对 content+expireAt 后删除，
 * 防止并发双提交与重发误删新码。
 */
@Component
@ConditionalOnProperty(name = "captcha.store", havingValue = "redis")
@ConditionalOnClass(StringRedisTemplate.class)
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

    private final StringRedisTemplate redis;

    public RedisValidateCodeStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void put(String key, ValidateCode code) {
        long ttl = Duration.between(Instant.now(), code.expireAt()).getSeconds();
        if (ttl <= 0) {
            // 已过期不写入（等同内存实现惰性清理后不可见）
            return;
        }
        redis.opsForHash().putAll(key, Map.of(FIELD_CONTENT, code.content(),
                FIELD_EXPIRE_AT, code.expireAt().toString()));
        redis.expire(key, Duration.ofSeconds(ttl));
    }

    @Override
    public Optional<ValidateCode> get(String key) {
        Map<Object, Object> entries = redis.opsForHash().entries(key);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        Object content = entries.get(FIELD_CONTENT);
        Object expireAt = entries.get(FIELD_EXPIRE_AT);
        if (content == null || expireAt == null) {
            return Optional.empty();
        }
        ValidateCode code;
        try {
            code = new ValidateCode((String) content, Instant.parse((String) expireAt));
        } catch (RuntimeException e) {
            log.warn("验证码反序列化失败, key={}: {}", key, e.toString());
            redis.delete(key);
            return Optional.empty();
        }
        if (code.isExpired()) {
            redis.delete(key);
            return Optional.empty();
        }
        return Optional.of(code);
    }

    @Override
    public boolean consume(String key, ValidateCode code) {
        Long removed = redis.execute(CONSUME_SCRIPT, Collections.singletonList(key),
                code.content(), code.expireAt().toString());
        return removed != null && removed > 0;
    }

    @Override
    public void remove(String key) {
        redis.delete(key);
    }
}
