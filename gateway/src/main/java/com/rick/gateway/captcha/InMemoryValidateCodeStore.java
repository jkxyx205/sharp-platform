package com.rick.gateway.captcha;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证码内存存储：单机默认实现，进程重启即丢失。
 */
@Component
@ConditionalOnProperty(name = "captcha.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryValidateCodeStore implements ValidateCodeStore {

    private final Map<String, ValidateCode> codes = new ConcurrentHashMap<>();

    @Override
    public void put(String key, ValidateCode code) {
        codes.put(key, code);
    }

    @Override
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

    @Override
    public boolean consume(String key, ValidateCode code) {
        return codes.remove(key, code);
    }

    @Override
    public void remove(String key) {
        codes.remove(key);
    }
}
