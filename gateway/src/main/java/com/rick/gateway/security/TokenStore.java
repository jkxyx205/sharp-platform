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

    /** token 绑定的用户信息 */
    public record UserInfo(Long userId, String mobile) {
    }

    /** token -> UserInfo */
    private final Map<String, UserInfo> tokens = new ConcurrentHashMap<>();

    public String create(Long userId, String mobile) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new UserInfo(userId, mobile));
        return token;
    }

    public Optional<String> findUsername(String token) {
        return Optional.ofNullable(tokens.get(token)).map(UserInfo::mobile);
    }

    public Optional<UserInfo> findUserInfo(String token) {
        return Optional.ofNullable(tokens.get(token));
    }
}
