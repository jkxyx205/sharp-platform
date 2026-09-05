package com.rick.platform.module.message.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * 会话 Principal 的 name 即 userId 字符串：
 * simpMessagingTemplate.convertAndSendToUser(userId.toString(), "/queue/message", ...)
 * 会解析为 /user/{userId}/queue/message 并命中该用户的所有会话。
 * userId 由 {@link UserAuthHandshakeInterceptor} 在握手阶段写入 attributes。
 */
@Component
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    private record UserIdPrincipal(String userId) implements Principal {
        @Override
        public String getName() {
            return userId;
        }
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String userId = (String) attributes.get(UserAuthHandshakeInterceptor.ATTR_USER_ID);
        return userId == null ? null : new UserIdPrincipal(userId);
    }
}
