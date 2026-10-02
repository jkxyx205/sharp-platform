package com.rick.gateway.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * token 配置：会话超时 {@link #ttl}。
 * <p>
 * 默认 7 天，{@link TokenStore#findUserInfo(String)} 命中时刷新 TTL（滑动续期），
 * 长时间无活动的 token 自动过期；多设备并存，每次登录签发独立 token。
 */
@Data
@ConfigurationProperties("token")
public class TokenProperties {

    /** token 会话超时（滑动窗口）：自最近一次活跃起经过该时长未使用即失效 */
    private Duration ttl = Duration.ofDays(7);
}
