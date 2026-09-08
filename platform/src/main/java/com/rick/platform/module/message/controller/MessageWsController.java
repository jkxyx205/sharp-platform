package com.rick.platform.module.message.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

/**
 * 接收页面发来的 STOMP 消息。前端发送：
 * <pre>
 *   stompClient.send("/app/echo", {}, "内容")
 * </pre>
 * 回显到发送者会话的 /user/queue/message（与服务端推送同一目的地，
 * broadcast = false 表示只回给发出该消息的会话，不广播到该用户的其他会话）。
 */
@Controller
public class MessageWsController {

    @MessageMapping("/echo")
    @SendToUser(destinations = "/queue/message", broadcast = false)
    public String echo(String payload) {
        return payload;
    }
}
