package com.rick.gateway.captcha;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 内存频率限制器：间隔、日上限、mobile 仅短信维度、image 跳过 mobile 维度。
 */
class InMemoryRateLimiterTest {

    private ValidateCodeProperties properties;
    private ValidateCodeProperties.TypeSpec smsSpec;
    private ValidateCodeProperties.TypeSpec imageSpec;

    @BeforeEach
    void setUp() {
        properties = new ValidateCodeProperties();
        properties.getRateLimit().setInterval(Duration.ofSeconds(60));
        properties.getRateLimit().setMobileDailyLimit(2);
        properties.getRateLimit().setDeviceDailyLimit(3);
        properties.getRateLimit().setIpDailyLimit(5);
        smsSpec = new ValidateCodeProperties.TypeSpec();
        smsSpec.setKind(CodeKind.SMS);
        imageSpec = new ValidateCodeProperties.TypeSpec();
        imageSpec.setKind(CodeKind.IMAGE);
    }

    @Test
    void firstSendPasses() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);
        assertDoesNotThrow(() -> limiter.check(smsSpec, "13800000000", "dev-1", "127.0.0.1").block());
    }

    @Test
    void mobileIntervalRejectedWithinWindow() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);
        limiter.check(smsSpec, "13800000000", "dev-1", "127.0.0.1").block();
        // 同手机号 60s 内再次发送 → 拒绝
        RateLimitException e = assertThrows(RateLimitException.class,
                () -> limiter.check(smsSpec, "13800000000", "dev-2", "192.168.0.1").block());
        assertTrue(e.getMessage().contains("60s"));
    }

    @Test
    void mobileIntervalPassesAfterWindow() {
        properties.getRateLimit().setInterval(Duration.ofMillis(1));
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);
        limiter.check(smsSpec, "13800000000", "dev-1", "127.0.0.1").block();
        sleep(5);
        // 窗口过后允许再次发送
        assertDoesNotThrow(() -> limiter.check(smsSpec, "13800000000", "dev-1", "127.0.0.1").block());
    }

    @Test
    void mobileDailyLimitExceeded() {
        properties.getRateLimit().setInterval(Duration.ofMillis(1));
        properties.getRateLimit().setMobileDailyLimit(2);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);
        limiter.check(smsSpec, "13800000000", "dev-1", "127.0.0.1").block();
        sleep(5);
        limiter.check(smsSpec, "13800000000", "dev-2", "127.0.0.2").block();
        sleep(5);
        // 第三次：mobile 日上限 2 已达
        RateLimitException e = assertThrows(RateLimitException.class,
                () -> limiter.check(smsSpec, "13800000000", "dev-3", "127.0.0.3").block());
        assertTrue(e.getMessage().contains("mobile"));
    }

    @Test
    void deviceDailyLimitExceeded() {
        properties.getRateLimit().setInterval(Duration.ofMillis(1));
        properties.getRateLimit().setDeviceDailyLimit(2);
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);
        limiter.check(imageSpec, null, "dev-1", "127.0.0.1").block();
        limiter.check(imageSpec, null, "dev-1", "127.0.0.2").block();
        // 第三次：设备日上限 2 已达
        RateLimitException e = assertThrows(RateLimitException.class,
                () -> limiter.check(imageSpec, null, "dev-1", "127.0.0.3").block());
        assertTrue(e.getMessage().contains("device"));
    }

    @Test
    void imageSkipsMobileDimension() {
        // 图片码 mobile=null：不校验 mobile 间隔/日上限，仅 device/ip
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);
        limiter.check(imageSpec, null, "dev-1", "127.0.0.1").block();
        // 同设备同 IP 连续两次图片：mobile 维度本就不参与，间隔只针对 mobile，故允许
        assertDoesNotThrow(() -> limiter.check(imageSpec, null, "dev-1", "127.0.0.1").block());
    }

    @Test
    void nullIpSkipsIpDimension() {
        InMemoryRateLimiter limiter = new InMemoryRateLimiter(properties);
        // ip 为 null 不应抛 NPE，仅跳过 IP 维度
        assertDoesNotThrow(() -> limiter.check(smsSpec, "13800000000", "dev-1", null).block());
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
