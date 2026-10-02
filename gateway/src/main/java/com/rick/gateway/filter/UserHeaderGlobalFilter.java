package com.rick.gateway.filter;

import com.rick.gateway.security.TokenResolver;
import com.rick.gateway.security.TokenStore;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证通过后，把 token 中的用户身份以请求头透传给下游服务；
 * 下游拦截器读取后放入用户上下文（见 platform UserContextHolder）。
 * 请求头用 set 覆盖写，防止客户端伪造。
 * <p>
 * TokenStore 为响应式接口（Redis 实现非阻塞），通过 flatMap 接入响应式链，
 * 避免 token 查询阻塞 Netty 事件循环。
 */
@Component
public class UserHeaderGlobalFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_MOBILE = "X-User-Mobile";

    private final TokenStore tokenStore;

    public UserHeaderGlobalFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // token 提取与认证过滤器同一逻辑：Authorization 头优先，回退 access_token 查询参数
        String token = TokenResolver.resolveToken(exchange.getRequest());
        if (token == null) {
            return chain.filter(exchange);
        }
        return tokenStore.findUserInfo(token)
                .flatMap(userInfo -> {
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
                })
                // token 不存在/已过期：不注入身份头，原样转发（认证过滤器已负责 401）
                .switchIfEmpty(Mono.defer(() -> chain.filter(exchange)));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
