package com.rick.gateway.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存 token 存储：登录时签发，请求时校验。
 */
@Component
public class TokenStore {

    /** token 绑定的用户信息；permissions 为登录时刻快照的权限列表 */
    public record UserInfo(Long userId, String mobile, List<String> permissions) {
    }

    /** token -> UserInfo */
    private final Map<String, UserInfo> tokens = new ConcurrentHashMap<>();

    public String create(Long userId, String mobile, List<String> permissions) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new UserInfo(userId, mobile, permissions));
        return token;
    }

    public Optional<UserInfo> findUserInfo(String token) {
        return Optional.ofNullable(tokens.get(token));
    }

    /** 退出登录：移除 token，使其立即失效 */
    public void remove(String token) {
        tokens.remove(token);
    }
}
