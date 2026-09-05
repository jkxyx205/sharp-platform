package com.rick.gateway.captcha;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ValidateCodeStoreTest {

    @Test
    void buildKeyByKind() {
        assertEquals("13800000000:dev-1:register",
                ValidateCodeStore.buildKey(CodeKind.SMS, "13800000000", "dev-1", "register"));
        assertEquals("dev-1:login", ValidateCodeStore.buildKey(CodeKind.IMAGE, null, "dev-1", "login"));
    }

    @Test
    void expiredCodeCleanedLazily() {
        ValidateCodeStore store = new ValidateCodeStore();
        store.put("k", new ValidateCode("123456", Instant.now().minusSeconds(1)));
        assertTrue(store.get("k").isEmpty());
        assertTrue(store.get("k").isEmpty());
    }

    @Test
    void consumeIsOneShot() {
        ValidateCodeStore store = new ValidateCodeStore();
        ValidateCode code = new ValidateCode("123456", Instant.now().plusSeconds(60));
        store.put("k", code);
        assertTrue(store.consume("k", code));
        assertFalse(store.consume("k", code));
        assertTrue(store.get("k").isEmpty());
    }
}
