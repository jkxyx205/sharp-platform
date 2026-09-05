package com.rick.gateway;

import com.rick.sms.core.Sender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

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
