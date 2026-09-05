package com.rick.gateway.captcha;

import com.rick.sms.core.ValidateCodeSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static com.rick.gateway.captcha.ValidateCodeService.VerifyResult.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ValidateCodeServiceTest {

    private ValidateCodeStore store;
    private ValidateCodeSender sender;
    private ValidateCodeService service;

    @BeforeEach
    void setUp() {
        ValidateCodeProperties properties = new ValidateCodeProperties();
        properties.setSignName("xx科技");
        ValidateCodeProperties.TypeSpec sms = new ValidateCodeProperties.TypeSpec();
        sms.setKind(CodeKind.SMS);
        sms.setTemplate("SMS_1");
        ValidateCodeProperties.TypeSpec image = new ValidateCodeProperties.TypeSpec();
        image.setKind(CodeKind.IMAGE);
        properties.setTypes(Map.of("register", sms, "login", image));
        store = new ValidateCodeStore();
        sender = mock(ValidateCodeSender.class);
        service = new ValidateCodeService(properties, store, sender);
    }

    @Test
    void smsIssueGeneratesSixDigitsStoresAndSends() {
        ValidateCode code = service.sendCode("register", "13800000000", "dev-1");
        assertEquals(6, code.content().length());
        assertTrue(code.content().matches("\\d{6}"));
        assertEquals(Optional.of(code), store.get("13800000000:dev-1:register"));
        verify(sender).send(eq("13800000000"), eq("xx科技"), eq("SMS_1"), eq(code.content()));
    }

    @Test
    void imageIssueGeneratesAlphanumericWithoutSending() {
        ValidateCode code = service.sendCode("login", null, "dev-1");
        assertEquals(4, code.content().length());
        assertTrue(code.content().matches("[2-9A-HJ-NP-Z]{4}"));
        assertEquals(Optional.of(code), store.get("dev-1:login"));
        verifyNoInteractions(sender);
    }

    @Test
    void typeExpireSecondsOverridesGlobal() {
        ValidateCodeProperties.TypeSpec spec = new ValidateCodeProperties.TypeSpec();
        spec.setKind(CodeKind.IMAGE);
        spec.setExpireSeconds(10L);
        ValidateCodeProperties properties = new ValidateCodeProperties();
        properties.setExpireSeconds(300);
        properties.setTypes(Map.of("login", spec));
        ValidateCodeService s = new ValidateCodeService(properties, new ValidateCodeStore(), sender);

        ValidateCode code = s.sendCode("login", null, "dev-1");
        long seconds = code.expireAt().getEpochSecond() - Instant.now().getEpochSecond();
        assertTrue(seconds > 5 && seconds <= 10, "expireAt=" + code.expireAt());
    }

    @Test
    void sendFailureRollsBackStoredCode() {
        doThrow(new RuntimeException("ali down")).when(sender).send(any(), any(), any(), any());
        assertThrows(RuntimeException.class, () -> service.sendCode("register", "13800000000", "dev-1"));
        assertTrue(store.get("13800000000:dev-1:register").isEmpty());
    }

    @Test
    void unknownTypeFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> service.sendCode("no-such-type", null, "dev-1"));
    }

    @Test
    void verifyStatesAndOneShot() {
        service.sendCode("register", "13800000000", "dev-1");
        String content = store.get("13800000000:dev-1:register").orElseThrow().content();

        assertEquals(MISMATCH, service.verify(CodeKind.SMS, "register", "13800000000", "dev-1", "xxxxxx"));
        // 错误不删码，可重试
        assertEquals(OK, service.verify(CodeKind.SMS, "register", "13800000000", "dev-1", content));
        // 一次性：成功后再验必失败
        assertEquals(NOT_FOUND_OR_EXPIRED, service.verify(CodeKind.SMS, "register", "13800000000", "dev-1", content));
    }

    @Test
    void expiredCodeFailsVerify() {
        store.put("13800000000:dev-1:register", new ValidateCode("123456", Instant.now().minusSeconds(1)));
        assertEquals(NOT_FOUND_OR_EXPIRED,
                service.verify(CodeKind.SMS, "register", "13800000000", "dev-1", "123456"));
    }

    @Test
    void imageMatchesCaseInsensitive() {
        service.sendCode("login", null, "dev-1");
        String content = store.get("dev-1:login").orElseThrow().content();
        assertEquals(OK, service.verify(CodeKind.IMAGE, "login", null, "dev-1", content.toLowerCase()));
    }
}
