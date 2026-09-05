package com.rick.platform.module.message.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP/SockJS 消息推送端点。前端（messageUrl = 网关地址 + /api/platform）：
 * <pre>
 *   new SockJS(messageUrl + "/ws/message?access_token=" + token)
 *   stompClient.subscribe("/user/queue/message", ...)
 * </pre>
 * 推送方：{@code simpMessagingTemplate.convertAndSendToUser(userId, "/queue/message", payload)}。
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final UserAuthHandshakeInterceptor userAuthHandshakeInterceptor;
    private final UserHandshakeHandler userHandshakeHandler;

    public WebSocketConfig(UserAuthHandshakeInterceptor userAuthHandshakeInterceptor,
                           UserHandshakeHandler userHandshakeHandler) {
        this.userAuthHandshakeInterceptor = userAuthHandshakeInterceptor;
        this.userHandshakeHandler = userHandshakeHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/message")
                .addInterceptors(userAuthHandshakeInterceptor)
                .setHandshakeHandler(userHandshakeHandler)
                // SockJS 会设 allowCredentials=true，此时 allowedOrigins 不能为 "*"（Spring 6 抛异常）；
                // 放开所有来源须用 setAllowedOriginPatterns("*")。经网关代理时 Origin 头会被透传，必须用此项。
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setUserDestinationPrefix("/user");
    }
}
