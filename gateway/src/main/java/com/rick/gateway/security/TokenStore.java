package com.rick.gateway.security;

import java.util.List;
import java.util.Optional;

/**
 * token 存储抽象：登录签发，请求校验，退出移除。
 * <p>
 * 按 {@code token.store} 选择实现：默认内存 {@link InMemoryTokenStore}（缺省），
 * 多实例共享用 {@link RedisTokenStore}（设 {@code token.store=redis}）。
 */
public interface TokenStore {

    /** token 绑定的用户信息；permissions 为登录时刻快照的权限列表 */
    record UserInfo(Long userId, String mobile, List<String> permissions) {
    }

    String create(Long userId, String mobile, List<String> permissions);

    Optional<UserInfo> findUserInfo(String token);

    /** 退出登录：移除 token，使其立即失效 */
    void remove(String token);
}
