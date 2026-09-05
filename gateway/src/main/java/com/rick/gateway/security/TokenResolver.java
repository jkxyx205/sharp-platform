package com.rick.gateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * token 提取：优先 Authorization: Bearer 请求头；
 * SockJS/WebSocket 握手无法自定义请求头，回退到 access_token 查询参数。
 */
public final class TokenResolver {

    public static final String BEARER_PREFIX = "Bearer ";
    public static final String QUERY_ACCESS_TOKEN = "access_token";

    private TokenResolver() {
    }

    /** 未携带或为空返回 null */
    public static String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        String accessToken = request.getQueryParams().getFirst(QUERY_ACCESS_TOKEN);
        return (accessToken != null && !accessToken.isBlank()) ? accessToken : null;
    }
}
