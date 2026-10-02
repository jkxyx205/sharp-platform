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
        ValidateCodeStore store = new InMemoryValidateCodeStore();
        store.put("k", new ValidateCode("123456", Instant.now().minusSeconds(1))).block();
        assertTrue(store.get("k").block() == null);
        assertTrue(store.get("k").block() == null);
    }

    @Test
    void consumeIsOneShot() {
        ValidateCodeStore store = new InMemoryValidateCodeStore();
        ValidateCode code = new ValidateCode("123456", Instant.now().plusSeconds(60));
        store.put("k", code).block();
        assertTrue(store.consume("k", code).block());
        assertFalse(store.consume("k", code).block());
        assertTrue(store.get("k").block() == null);
    }
}
