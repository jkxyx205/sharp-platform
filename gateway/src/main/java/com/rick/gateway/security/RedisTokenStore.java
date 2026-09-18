package com.rick.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * token Redis 存储：多实例共享、重启不丢失，支持水平扩容与统一登出。
 * <p>
 * 以 token 为 key 存 Hash（userId/mobile/permissions），不设 TTL——与内存实现一致，
 * token 直到 {@link #remove(String)} 退出才失效；如需自动过期可在 put 后追加 expire。
 * permissions 以换行分隔存储（权限名为简单标识符，不含换行）。
 */
@Component
@ConditionalOnProperty(name = "token.store", havingValue = "redis")
@ConditionalOnClass(StringRedisTemplate.class)
public class RedisTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_MOBILE = "mobile";
    private static final String FIELD_PERMISSIONS = "permissions";
    /** 权限分隔符：权限名为简单标识符，不会包含换行 */
    private static final String PERM_DELIMITER = "\n";

    private final StringRedisTemplate redis;

    public RedisTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public String create(Long userId, String mobile, List<String> permissions) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, String> fields = new HashMap<>(3);
        fields.put(FIELD_USER_ID, String.valueOf(userId));
        fields.put(FIELD_MOBILE, mobile);
        fields.put(FIELD_PERMISSIONS, String.join(PERM_DELIMITER, permissions));
        redis.opsForHash().putAll(token, fields);
        return token;
    }

    @Override
    public Optional<UserInfo> findUserInfo(String token) {
        Map<Object, Object> entries = redis.opsForHash().entries(token);
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        Object userId = entries.get(FIELD_USER_ID);
        Object mobile = entries.get(FIELD_MOBILE);
        Object permissions = entries.get(FIELD_PERMISSIONS);
        if (userId == null || mobile == null || permissions == null) {
            return Optional.empty();
        }
        try {
            String permsStr = (String) permissions;
            List<String> perms = permsStr.isEmpty()
                    ? List.of()
                    : List.of(permsStr.split(PERM_DELIMITER));
            return Optional.of(new UserInfo(Long.parseLong((String) userId), (String) mobile, perms));
        } catch (RuntimeException e) {
            log.warn("token 反序列化失败, token={}: {}", token, e.toString());
            redis.delete(token);
            return Optional.empty();
        }
    }

    @Override
    public void remove(String token) {
        redis.delete(token);
    }
}
