package com.rick.gateway.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 token 存储：登录时签发，请求时校验。
 */
@Component
public class TokenStore {

    /** token -> username */
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    public String create(String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, username);
        return token;
    }

    public Optional<String> findUsername(String token) {
        return Optional.ofNullable(tokens.get(token));
    }
}
