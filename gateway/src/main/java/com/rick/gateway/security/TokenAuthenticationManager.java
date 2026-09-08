package com.rick.gateway.security;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 校验请求头中的 token 是否存在于内存存储中。
 */
@Component
public class TokenAuthenticationManager implements ReactiveAuthenticationManager {

    private final TokenStore tokenStore;

    public TokenAuthenticationManager(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        return Mono.justOrEmpty(tokenStore.findUserInfo(token))
                // principal 用 mobile（唯一登录标识），authorities 来自登录时存入的权限快照
                .<Authentication>map(info -> new UsernamePasswordAuthenticationToken(info.mobile(), token,
                        info.permissions().stream().map(SimpleGrantedAuthority::new).toList()))
                // 无效 token 必须以 AuthenticationException 形式返回，空 Mono 会被视为无可用 provider
                .switchIfEmpty(Mono.error(new BadCredentialsException("invalid token")));
    }
}
