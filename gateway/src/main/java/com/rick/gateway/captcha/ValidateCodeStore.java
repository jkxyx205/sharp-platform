package com.rick.gateway.captcha;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码内存存储：发送时写入，业务过滤器校验。
 * key：短信为 mobile:deviceId:type，图片为 deviceId:type。
 */
@Component
public class ValidateCodeStore {

    private final Map<String, ValidateCode> codes = new ConcurrentHashMap<>();

    public void put(String key, ValidateCode code) {
        codes.put(key, code);
    }

    /**
     * 读取不删除；过期条目惰性清理。
     */
    public Optional<ValidateCode> get(String key) {
        ValidateCode code = codes.get(key);
        if (code == null) {
            return Optional.empty();
        }
        if (code.isExpired()) {
            codes.remove(key, code);
            return Optional.empty();
        }
        return Optional.of(code);
    }

    /**
     * 原子消费：目标仍存在时才删除（并发双提交只允许一个通过）。
     */
    public boolean consume(String key, ValidateCode code) {
        return codes.remove(key, code);
    }

    public void remove(String key) {
        codes.remove(key);
    }

    public static String buildKey(CodeKind kind, String mobile, String deviceId, String type) {
        return kind == CodeKind.IMAGE
                ? deviceId + ":" + type
                : mobile + ":" + deviceId + ":" + type;
    }
}
