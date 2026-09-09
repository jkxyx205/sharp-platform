package com.rick.gateway.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 校验请求头中的 token：通过 {@link ReactiveUserDetailsService}（token 即"用户名"）
 * 加载 UserDetails（身份 + 权限），成功则构建已认证的 Authentication。
 */
@Component
public class TokenAuthenticationManager implements ReactiveAuthenticationManager {

    private final ReactiveUserDetailsService userDetailsService;

    public TokenAuthenticationManager(ReactiveUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return userDetailsService.findByUsername(token)
                // principal 为 UserDetails（getName() = mobile），authorities 来自登录时存入的权限快照
                .<Authentication>map(details -> new UsernamePasswordAuthenticationToken(
                        details, token, details.getAuthorities()))
                // 无效 token 必须以 AuthenticationException 形式返回，空 Mono 会被视为无可用 provider
                .switchIfEmpty(Mono.error(new BadCredentialsException("invalid token")));
    }
}
