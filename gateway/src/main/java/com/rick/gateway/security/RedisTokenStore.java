package com.rick.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * token Redis 存储：多实例共享、重启不丢失，支持水平扩容与统一登出。
 * <p>
 * 以 token 为 key 存 Hash（userId/mobile/permissions），写入即设 TTL（{@link TokenProperties#getTtl()}），
 * {@link #findUserInfo(String)} 命中时刷新 TTL（滑动续期），闲置超时自动过期。
 * permissions 以换行分隔存储（权限名为简单标识符，不含换行）。
 * <p>
 * 使用 {@link ReactiveStringRedisTemplate} 非阻塞响应式 IO，避免阻塞 Netty 事件循环。
 */
@Component
@ConditionalOnProperty(name = "token.store", havingValue = "redis")
@ConditionalOnClass(ReactiveStringRedisTemplate.class)
public class RedisTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_MOBILE = "mobile";
    private static final String FIELD_PERMISSIONS = "permissions";
    /** 权限分隔符：权限名为简单标识符，不会包含换行 */
    private static final String PERM_DELIMITER = "\n";

    private final ReactiveStringRedisTemplate redis;
    private final Duration ttl;

    public RedisTokenStore(ReactiveStringRedisTemplate redis, TokenProperties properties) {
        this.redis = redis;
        this.ttl = properties.getTtl();
    }

    @Override
    public Mono<String> create(Long userId, String mobile, List<String> permissions) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, String> fields = Map.of(
                FIELD_USER_ID, String.valueOf(userId),
                FIELD_MOBILE, mobile == null ? "" : mobile,
                FIELD_PERMISSIONS, String.join(PERM_DELIMITER, permissions));
        return redis.opsForHash().putAll(token, fields)
                .then(redis.expire(token, ttl))
                .thenReturn(token);
    }

    @Override
    public Mono<UserInfo> findUserInfo(String token) {
        return redis.opsForHash().multiGet(token, List.of(FIELD_USER_ID, FIELD_MOBILE, FIELD_PERMISSIONS))
                .flatMap(values -> {
                    if (values == null || values.size() < 3) {
                        return Mono.<UserInfo>empty();
                    }
                    String userId = stringOf(values.get(0));
                    String mobile = stringOf(values.get(1));
                    String permissions = stringOf(values.get(2));
                    if (userId == null || mobile == null || permissions == null) {
                        return Mono.<UserInfo>empty();
                    }
                    try {
                        List<String> perms = permissions.isEmpty()
                                ? List.of()
                                : List.of(permissions.split(PERM_DELIMITER));
                        UserInfo info = new UserInfo(Long.parseLong(userId), mobile, perms);
                        // 滑动续期：刷新 TTL，活跃会话不因闲置超时失效
                        return redis.expire(token, ttl).thenReturn(info);
                    } catch (RuntimeException e) {
                        log.warn("token 反序列化失败, token={}: {}", token, e.toString());
                        return redis.delete(token).then(Mono.<UserInfo>empty());
                    }
                });
    }

    private static String stringOf(Object value) {
        return value == null ? null : value.toString();
    }

    @Override
    public Mono<Void> remove(String token) {
        return redis.delete(token).then();
    }
}
