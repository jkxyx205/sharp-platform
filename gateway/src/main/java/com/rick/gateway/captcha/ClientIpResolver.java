package com.rick.gateway.captcha;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetSocketAddress;

/**
 * 客户端 IP 提取：优先 X-Forwarded-For 首段（网关前置 SLB/Nginx 注入），
 * 回退到 TCP 远端地址。用于验证码发送频率限制的 IP 维度。
 */
public final class ClientIpResolver {

    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";

    private ClientIpResolver() {
    }

    /** 取不到返回 null */
    public static String resolve(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst(HEADER_X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            String trimmed = first.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        InetSocketAddress remote = request.getRemoteAddress();
        return remote == null ? null : remote.getHostString();
    }
}
