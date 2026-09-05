package com.rick.platform.module.message.config;

import com.rick.platform.config.UserContextWebConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手鉴权：认证已在网关完成（access_token 查询参数 / Bearer 头），
 * 这里只信任网关透传的 X-User-Id 请求头；头缺失说明请求绕过了网关，拒绝握手。
 * userId 写入 attributes，供 {@link UserHandshakeHandler} 构建会话 Principal。
 */
@Component
public class UserAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String userId = request.getHeaders().getFirst(UserContextWebConfig.HEADER_USER_ID);
        if (userId == null || userId.isBlank()) {
            // 拦截器返回 false 时 Spring 不自动写状态码，需显式置 403
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(ATTR_USER_ID, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
