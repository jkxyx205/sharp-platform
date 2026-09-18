package com.rick.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 token 存储：单机默认实现，进程重启即失效。
 */
@Component
@ConditionalOnProperty(name = "token.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenStore implements TokenStore {

    /** token -> UserInfo */
    private final Map<String, UserInfo> tokens = new ConcurrentHashMap<>();

    @Override
    public String create(Long userId, String mobile, List<String> permissions) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new UserInfo(userId, mobile, permissions));
        return token;
    }

    @Override
    public Optional<UserInfo> findUserInfo(String token) {
        return Optional.ofNullable(tokens.get(token));
    }

    @Override
    public void remove(String token) {
        tokens.remove(token);
    }
}
