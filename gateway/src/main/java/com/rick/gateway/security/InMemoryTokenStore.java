package com.rick.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 token 存储：单机默认实现，进程重启即失效。
 * <p>
 * 写入即设 TTL（{@link TokenProperties#getTtl()}），{@link #findUserInfo(String)} 命中刷新 TTL（滑动续期），
 * 过期 token 惰性清理。多设备并存：每次登录签发独立 token。
 */
@Component
@ConditionalOnProperty(name = "token.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenStore implements TokenStore {

    private record Entry(UserInfo info, Instant expireAt) {
    }

    /** token -> Entry */
    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();
    private final Duration ttl;

    /** 无参构造：委托默认配置（默认 TTL），供测试与兜底使用 */
    public InMemoryTokenStore() {
        this(new TokenProperties());
    }

    public InMemoryTokenStore(TokenProperties properties) {
        this.ttl = properties.getTtl();
    }

    @Override
    public Mono<String> create(Long userId, String mobile, List<String> permissions) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new Entry(new UserInfo(userId, mobile, permissions), Instant.now().plus(ttl)));
        return Mono.just(token);
    }

    @Override
    public Mono<UserInfo> findUserInfo(String token) {
        return Mono.defer(() -> {
            Entry entry = tokens.get(token);
            if (entry == null || Instant.now().isAfter(entry.expireAt())) {
                if (entry != null) {
                    tokens.remove(token, entry);
                }
                return Mono.empty();
            }
            // 滑动续期：刷新过期时刻
            tokens.put(token, new Entry(entry.info(), Instant.now().plus(ttl)));
            return Mono.just(entry.info());
        });
    }

    @Override
    public Mono<Void> remove(String token) {
        return Mono.fromRunnable(() -> tokens.remove(token)).then();
    }
}
