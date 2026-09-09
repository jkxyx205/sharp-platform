package com.rick.gateway.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 基于 token 的用户加载：{@code findByUsername} 的"用户名"即 token。
 * 从 {@link TokenStore} 读取登录时存入的身份（mobile）与权限快照构建 UserDetails；
 * token 不存在返回空 Mono，由认证管理器转为 BadCredentialsException。
 */
@Component
public class TokenUserDetailsService implements ReactiveUserDetailsService {

    private final TokenStore tokenStore;

    public TokenUserDetailsService(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public Mono<UserDetails> findByUsername(String token) {
        return Mono.justOrEmpty(tokenStore.findUserInfo(token))
                .map(info -> User.withUsername(info.mobile())
                        // token 认证不校验密码，占位空串（builder 要求非 null）
                        .password("")
                        .authorities(info.permissions().stream()
                                .map(SimpleGrantedAuthority::new).toList())
                        .build());
    }
}
