package com.rick.api.platform;

/**
 * 站内消息推送：其他服务通过 Dubbo 调用，由 platform 转发给目标用户的
 * WebSocket 连接（/user/{userId}/queue/message）。
 */
public interface MessageService {

    /**
     * 向指定用户推送消息。
     *
     * @param userId  目标用户 id
     * @param message 消息内容（业务方自行约定格式，如 JSON 字符串）
     */
    void sendToUser(Long userId, String message);
}
