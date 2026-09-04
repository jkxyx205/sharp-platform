package com.rick.gateway.filter;

import com.rick.gateway.security.TokenStore;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证通过后，把 token 中的用户身份以请求头透传给下游服务；
 * 下游拦截器读取后放入用户上下文（见 platform UserContextHolder）。
 * 请求头用 set 覆盖写，防止客户端伪造。
 */
@Component
public class UserHeaderGlobalFilter implements GlobalFilter, Ordered {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_MOBILE = "X-User-Mobile";

    private final TokenStore tokenStore;

    public UserHeaderGlobalFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            TokenStore.UserInfo userInfo = tokenStore
                    .findUserInfo(authorization.substring(BEARER_PREFIX.length()))
                    .orElse(null);
            if (userInfo != null) {
                ServerHttpRequest request = exchange.getRequest().mutate()
                        .headers(headers -> {
                            if (userInfo.userId() != null) {
                                headers.set(HEADER_USER_ID, String.valueOf(userInfo.userId()));
                            }
                            if (userInfo.mobile() != null) {
                                headers.set(HEADER_USER_MOBILE, userInfo.mobile());
                            }
                        })
                        .build();
                return chain.filter(exchange.mutate().request(request).build());
            }
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
