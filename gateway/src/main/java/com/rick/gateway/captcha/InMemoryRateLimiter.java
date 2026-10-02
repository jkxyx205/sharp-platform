package com.rick.gateway.captcha;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内存频率限制器：单机默认实现。
 * <p>
 * 维度（手机号仅短信、设备/IP 通用）：
 * <ul>
 *   <li>同手机号两次发送间隔 ≥ {@code captcha.rate-limit.interval}（默认 60s）</li>
 *   <li>单手机号/设备/IP 每日发送上限（按 epoch-day 自然分桶，跨天重置）</li>
 * </ul>
 * 纯 ConcurrentHashMap 操作，非阻塞，留在事件循环线程无妨。
 */
@Component
@ConditionalOnProperty(name = "captcha.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryRateLimiter implements CodeRateLimiter {

    private static final String DIM_MOBILE = "mobile";
    private static final String DIM_DEVICE = "device";
    private static final String DIM_IP = "ip";

    private final ValidateCodeProperties.RateLimit cfg;

    /** mobile -> 最近一次发送时刻，用于间隔判定 */
    private final Map<String, Instant> lastMobileSend = new ConcurrentHashMap<>();

    /** "{dim}:{value}" -> 当日计数（含 epoch-day，跨天重置） */
    private final Map<String, DayCounter> dailyCounts = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(ValidateCodeProperties properties) {
        this.cfg = properties.getRateLimit();
    }

    @Override
    public Mono<Void> check(ValidateCodeProperties.TypeSpec spec, String mobile, String deviceId, String ip) {
        return Mono.defer(() -> {
            if (spec.getKind() == CodeKind.SMS && mobile != null && !mobile.isBlank()) {
                checkMobileInterval(mobile);
                bumpDaily(DIM_MOBILE, mobile, cfg.getMobileDailyLimit());
            }
            if (deviceId != null && !deviceId.isBlank()) {
                bumpDaily(DIM_DEVICE, deviceId, cfg.getDeviceDailyLimit());
            }
            if (ip != null && !ip.isBlank()) {
                bumpDaily(DIM_IP, ip, cfg.getIpDailyLimit());
            }
            return Mono.<Void>empty();
        });
    }

    /** 同手机号间隔校验：compute 原子更新，间隔未到抛 RateLimitException */
    private void checkMobileInterval(String mobile) {
        Instant now = Instant.now();
        Instant prev = lastMobileSend.compute(mobile, (k, v) ->
                v == null || !Duration.between(v, now).minus(cfg.getInterval()).isNegative()
                        ? now : v);
        if (!now.equals(prev)) {
            throw new RateLimitException("同一手机号 " + cfg.getInterval().getSeconds() + "s 内仅可发送一次");
        }
    }

    private void bumpDaily(String dim, String value, int limit) {
        DayCounter counter = dailyCounts.computeIfAbsent(dim + ":" + value, k -> new DayCounter());
        if (!counter.tryBump(today(), limit)) {
            throw new RateLimitException(dim + " 当日发送次数已达上限");
        }
    }

    private static long today() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    /** 当日计数：epoch-day 变化时重置；超过上限回滚 */
    private static final class DayCounter {
        final AtomicInteger count = new AtomicInteger();
        final AtomicLong day = new AtomicLong(-1);

        boolean tryBump(long today, int limit) {
            if (day.get() != today) {
                synchronized (this) {
                    if (day.get() != today) {
                        day.set(today);
                        count.set(0);
                    }
                }
            }
            int n = count.incrementAndGet();
            if (n > limit) {
                count.decrementAndGet();
                return false;
            }
            return true;
        }
    }
}
