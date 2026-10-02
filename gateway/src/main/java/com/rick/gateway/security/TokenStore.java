package com.rick.gateway.security;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * token 存储抽象：登录签发，请求校验，退出移除。
 * <p>
 * 按 {@code token.store} 选择实现：默认内存 {@link InMemoryTokenStore}（缺省），
 * 多实例共享用 {@link RedisTokenStore}（设 {@code token.store=redis}）。
 * <p>
 * 方法返回 Mono：Redis 实现为非阻塞响应式 IO，避免阻塞 Netty 事件循环。
 */
public interface TokenStore {

    /** token 绑定的用户信息；permissions 为登录时刻快照的权限列表 */
    record UserInfo(Long userId, String mobile, List<String> permissions) {
    }

    /** 签发 token，绑定用户身份；实现负责写入会话 TTL */
    Mono<String> create(Long userId, String mobile, List<String> permissions);

    /**
     * 读取 token 绑定的用户信息；不存在或已过期返回空 Mono。
     * 命中时刷新 TTL（滑动续期），使活跃会话不因闲置超时失效。
     */
    Mono<UserInfo> findUserInfo(String token);

    /** 退出登录：移除 token，使其立即失效 */
    Mono<Void> remove(String token);
}
