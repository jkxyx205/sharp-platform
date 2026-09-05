package com.rick.gateway;

import com.rick.sms.core.Sender;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

// 手动冒烟用例：需要 .env.sms.properties 中配置真实短信 AK 才能通过，不随构建执行
@Disabled("requires real sms access key")
@SpringBootTest
public class SmsTest {

    @Autowired
    private Sender sender;

    @Test
    public void testSend() {
        Map<String, String> params = new HashMap<>(1);
        params.put("code", "123456");
        sender.send("18888888888", "XXX科技", "SMS_1823350004", params);
    }
}
