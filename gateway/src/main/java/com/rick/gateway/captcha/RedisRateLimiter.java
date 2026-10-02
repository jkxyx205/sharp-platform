package com.rick.gateway.captcha;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Redis 频率限制器：多实例共享，分布式计数。
 * <p>
 * 维度与内存版一致：同手机号间隔（短信）、手机号/设备/IP 日上限。
 * <p>
 * 计数用 Lua「INCR + 首次 EXPIRE + 超限返回 -1」原子完成，避免 INCR 与 EXPIRE 之间的竞态；
 * 日计数 key 含 epoch-day 自然分桶，TTL 86400s 后随 Redis 自动清理。
 * 使用 {@link ReactiveStringRedisTemplate} 非阻塞响应式 IO。
 */
@Component
@ConditionalOnProperty(name = "captcha.store", havingValue = "redis")
@ConditionalOnClass(ReactiveStringRedisTemplate.class)
public class RedisRateLimiter implements CodeRateLimiter {

    /**
     * KEYS[1]=计数 key ARGV[1]=上限 ARGV[2]=TTL(秒)；
     * 首次写入设 TTL，超过上限返回 -1，否则返回当前计数。
     */
    private static final DefaultRedisScript<Long> BUMP_SCRIPT =
            new DefaultRedisScript<>(
                    "local c = redis.call('INCR', KEYS[1]) " +
                    "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end " +
                    "if c > tonumber(ARGV[1]) then return -1 end " +
                    "return c",
                    Long.class);

    private final ReactiveStringRedisTemplate redis;
    private final ValidateCodeProperties.RateLimit cfg;

    public RedisRateLimiter(ReactiveStringRedisTemplate redis, ValidateCodeProperties properties) {
        this.redis = redis;
        this.cfg = properties.getRateLimit();
    }

    @Override
    public Mono<Void> check(ValidateCodeProperties.TypeSpec spec, String mobile, String deviceId, String ip) {
        Mono<Void> chain = Mono.empty();
        if (spec.getKind() == CodeKind.SMS && mobile != null && !mobile.isBlank()) {
            long day = today();
            long intervalSec = cfg.getInterval().getSeconds();
            chain = chain
                    .then(bumpAndCheck("rl:interval:mobile:" + mobile, 1L, intervalSec,
                            "同一手机号 " + intervalSec + "s 内仅可发送一次"))
                    .then(bumpAndCheck("rl:day:mobile:" + mobile + ":" + day,
                            cfg.getMobileDailyLimit(), 86400L, "手机号当日发送次数已达上限"));
        }
        if (deviceId != null && !deviceId.isBlank()) {
            long day = today();
            chain = chain.then(bumpAndCheck("rl:day:device:" + deviceId + ":" + day,
                    cfg.getDeviceDailyLimit(), 86400L, "设备当日发送次数已达上限"));
        }
        if (ip != null && !ip.isBlank()) {
            long day = today();
            chain = chain.then(bumpAndCheck("rl:day:ip:" + ip + ":" + day,
                    cfg.getIpDailyLimit(), 86400L, "IP 当日发送次数已达上限"));
        }
        return chain;
    }

    /** INCR 计数并校验上限，超限以 RateLimitException 信号化传播 */
    private Mono<Void> bumpAndCheck(String key, long limit, long ttlSeconds, String errMsg) {
        return redis.execute(BUMP_SCRIPT, List.of(key), String.valueOf(limit), String.valueOf(ttlSeconds))
                .next()
                .flatMap(n -> n != null && n < 0
                        ? Mono.<Void>error(new RateLimitException(errMsg))
                        : Mono.<Void>empty());
    }

    private static long today() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }
}
