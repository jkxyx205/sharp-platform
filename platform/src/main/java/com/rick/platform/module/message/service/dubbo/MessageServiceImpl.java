package com.rick.platform.module.message.service.dubbo;

import com.rick.api.platform.MessageService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Dubbo 消息推送实现：固定推送到 /user/{userId}/queue/message，
 * 前端订阅 /user/queue/message 即可收到（见 WebSocketConfig）。
 */
@DubboService
public class MessageServiceImpl implements MessageService {

    public static final String USER_MESSAGE_DESTINATION = "/queue/message";

    private final SimpMessagingTemplate simpMessagingTemplate;

    public MessageServiceImpl(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Override
    public void sendToUser(Long userId, String message) {
        simpMessagingTemplate.convertAndSendToUser(String.valueOf(userId), USER_MESSAGE_DESTINATION, message);
    }
}
