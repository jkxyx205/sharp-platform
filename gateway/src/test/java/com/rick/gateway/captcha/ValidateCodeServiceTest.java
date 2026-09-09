package com.rick.gateway.captcha;

import com.rick.sms.core.ValidateCodeSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
        service = new ValidateCodeService(properties, store, sender, Map.of());
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
        ValidateCodeService s = new ValidateCodeService(properties, new ValidateCodeStore(), sender, Map.of());

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
    void verifyDoesNotConsumeCode() {
        service.sendCode("register", "13800000000", "dev-1");
        String content = store.get("13800000000:dev-1:register").orElseThrow().content();

        assertEquals(MISMATCH, service.verify(CodeKind.SMS, "register", "13800000000", "dev-1", "xxxxxx"));
        // 错误不删码，可重试
        assertEquals(OK, service.verify(CodeKind.SMS, "register", "13800000000", "dev-1", content));
        // 校验通过也不删码（业务失败可重试），再验仍 OK
        assertEquals(OK, service.verify(CodeKind.SMS, "register", "13800000000", "dev-1", content));
        // 业务成功后 consume 才失效
        service.consume(CodeKind.SMS, "register", "13800000000", "dev-1");
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

    // ---------- 预检查（CodePreHandler） ----------

    /** 构造带 preHandler 的独立 service：handler 行为由入参脚本控制 */
    private ValidateCodeService guardedService(CodePreHandler handler) {
        ValidateCodeProperties properties = new ValidateCodeProperties();
        properties.setSignName("xx科技");
        ValidateCodeProperties.TypeSpec sms = new ValidateCodeProperties.TypeSpec();
        sms.setKind(CodeKind.SMS);
        sms.setTemplate("SMS_1");
        sms.setPreHandler("testPreHandler");
        properties.setTypes(Map.of("register", sms));
        return new ValidateCodeService(properties, new ValidateCodeStore(), sender,
                Map.of("testPreHandler", handler));
    }

    @Test
    void preHandlerTrueSendsCode() {
        AtomicReference<String> checked = new AtomicReference<>();
        ValidateCodeService s = guardedService(mobile -> {
            checked.set(mobile);
            return true;
        });

        ValidateCode code = s.sendCode("register", "13800000000", "dev-1");

        assertEquals("13800000000", checked.get());
        verify(sender).send(eq("13800000000"), eq("xx科技"), eq("SMS_1"), eq(code.content()));
    }

    @Test
    void preHandlerFalseRejectsWithDefaultMessageAndSendsNothing() {
        ValidateCodeService s = guardedService(mobile -> false);

        PreHandlerException e = assertThrows(PreHandlerException.class,
                () -> s.sendCode("register", "13800000000", "dev-1"));

        assertEquals("当前业务不允许发送验证码", e.getMessage());
        verifyNoInteractions(sender);
    }

    @Test
    void preHandlerExceptionPropagatesWithBusinessMessage() {
        ValidateCodeService s = guardedService(mobile -> {
            throw new PreHandlerException("该手机号已注册");
        });

        PreHandlerException e = assertThrows(PreHandlerException.class,
                () -> s.sendCode("register", "13800000000", "dev-1"));

        assertEquals("该手机号已注册", e.getMessage());
        verifyNoInteractions(sender);
    }

    @Test
    void preHandlerSkippedWhenMobileAbsent() {
        // 图片码 mobile=null（ImageCodeController 链路），配置了 preHandler 也不执行
        ValidateCodeProperties properties = new ValidateCodeProperties();
        ValidateCodeProperties.TypeSpec image = new ValidateCodeProperties.TypeSpec();
        image.setKind(CodeKind.IMAGE);
        image.setPreHandler("testPreHandler");
        properties.setTypes(Map.of("login", image));
        AtomicReference<String> checked = new AtomicReference<>();
        ValidateCodeService s = new ValidateCodeService(properties, new ValidateCodeStore(), sender,
                Map.of("testPreHandler", mobile -> {
                    checked.set(mobile);
                    return true;
                }));

        assertNotNull(s.sendCode("login", null, "dev-1"));
        assertNull(checked.get());
    }

    @Test
    void afterPropertiesSetFailsOnUnknownPreHandlerBean() {
        // properties 沿用 guardedService 的配置（preHandler=testPreHandler），但容器里没有该 bean
        ValidateCodeProperties properties = new ValidateCodeProperties();
        ValidateCodeProperties.TypeSpec sms = new ValidateCodeProperties.TypeSpec();
        sms.setKind(CodeKind.SMS);
        sms.setTemplate("SMS_1");
        sms.setPreHandler("cegisterCodePreHandler"); // 模拟 yml 笔误
        properties.setTypes(Map.of("register", sms));
        ValidateCodeService s = new ValidateCodeService(properties, new ValidateCodeStore(), sender, Map.of());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, s::afterPropertiesSet);
        assertTrue(e.getMessage().contains("cegisterCodePreHandler"));
    }

    @Test
    void afterPropertiesSetPassesWithConfiguredHandlers() {
        assertDoesNotThrow(guardedService(mobile -> true)::afterPropertiesSet);
    }
}
