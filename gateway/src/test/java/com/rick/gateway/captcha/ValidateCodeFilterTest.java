package com.rick.gateway.captcha;

import com.rick.gateway.security.TokenStore;
import com.rick.gateway.security.User;
import com.rick.sms.core.ValidateCodeSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * 过滤器逻辑测试：无认证上下文（user=null），
 * mobile 走 query 参数兜底或自定义解析方法。
 */
class ValidateCodeFilterTest {

    public static class TestResolver {
        public static String getMobile(User user, String type) {
            return "17799999999";
        }
    }

    private ValidateCodeStore store;
    private ValidateCodeFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        ValidateCodeProperties properties = new ValidateCodeProperties();
        properties.setSignName("xx科技");
        ValidateCodeProperties.TypeSpec sms = new ValidateCodeProperties.TypeSpec();
        sms.setKind(CodeKind.SMS);
        sms.setTemplate("SMS_T");
        ValidateCodeProperties.TypeSpec image = new ValidateCodeProperties.TypeSpec();
        image.setKind(CodeKind.IMAGE);
        properties.setTypes(Map.of("register", sms, "login", image));

        properties.setRules(List.of(
                rule("/biz/register", null, null),                       // type 默认取末段 register
                rule("/biz/login", "login", null),
                rule("/biz/custom", "register", TestResolver.class.getName() + ".getMobile")));
        properties.afterPropertiesSet();

        store = new ValidateCodeStore();
        ValidateCodeService service =
                new ValidateCodeService(properties, store, mock(ValidateCodeSender.class));
        filter = new ValidateCodeFilter(properties, service, new TokenStore(), mock(ApplicationContext.class));
    }

    private static ValidateCodeProperties.Rule rule(String url, String type, String mobile) {
        ValidateCodeProperties.Rule r = new ValidateCodeProperties.Rule();
        r.setUrl(url);
        r.setType(type);
        r.setMobile(mobile);
        return r;
    }

    /** 执行过滤器，返回 exchange；chain 模拟下游置 200，故 200 = 校验通过，400 = 被过滤器拒绝 */
    private MockServerWebExchange run(String uri, String... headerPairs) {
        return runWithStatus(uri, HttpStatus.OK, headerPairs);
    }

    /** 模拟下游业务返回指定状态码（非 2xx = 业务失败） */
    private MockServerWebExchange runWithStatus(String uri, HttpStatus downstreamStatus, String... headerPairs) {
        var builder = MockServerHttpRequest.get(uri);
        for (int i = 0; i < headerPairs.length; i += 2) {
            builder.header(headerPairs[i], headerPairs[i + 1]);
        }
        MockServerWebExchange exchange = MockServerWebExchange.from(builder);
        filter.filter(exchange, e -> {
            e.getResponse().setStatusCode(downstreamStatus);
            return Mono.empty();
        }).block();
        return exchange;
    }

    private void assertBody(MockServerWebExchange exchange, String expected) {
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body != null && body.contains(expected), "body=" + body);
    }

    @Test
    void unmatchedUrlPassesThrough() {
        MockServerWebExchange exchange = run("/other/path");
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    void missingDeviceIdRejected() {
        MockServerWebExchange exchange = run("/biz/register?code=123456&mobile=13800000000");
        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertBody(exchange, "缺少设备标识");
    }

    @Test
    void missingCodeRejected() {
        MockServerWebExchange exchange = run("/biz/register?mobile=13800000000", "deviceId", "dev-1");
        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
        assertBody(exchange, "验证码不能为空");
    }

    @Test
    void smsQueryMobileFallback() {
        store.put("13800000000:dev-1:register", code("123456"));
        MockServerWebExchange exchange =
                run("/biz/register?mobile=13800000000&code=123456", "deviceId", "dev-1");
        assertEquals(HttpStatus.OK, exchange.getResponse().getStatusCode());
    }

    @Test
    void verifiedCodeIsOneShot() {
        store.put("13800000000:dev-1:register", code("123456"));
        assertEquals(HttpStatus.OK,
                run("/biz/register?mobile=13800000000&code=123456", "deviceId", "dev-1").getResponse().getStatusCode());
        MockServerWebExchange second = run("/biz/register?mobile=13800000000&code=123456", "deviceId", "dev-1");
        assertEquals(HttpStatus.BAD_REQUEST, second.getResponse().getStatusCode());
        assertBody(second, "验证码不存在或已失效");
    }

    @Test
    void businessFailureKeepsCodeRetryable() {
        store.put("13800000000:dev-1:register", code("123456"));
        // 验证码正确但下游业务失败（409）：不消费验证码
        assertEquals(HttpStatus.CONFLICT,
                runWithStatus("/biz/register?mobile=13800000000&code=123456", HttpStatus.CONFLICT,
                        "deviceId", "dev-1").getResponse().getStatusCode());
        // 同一验证码可重试，业务成功（200）后才消费
        assertEquals(HttpStatus.OK,
                run("/biz/register?mobile=13800000000&code=123456", "deviceId", "dev-1").getResponse().getStatusCode());
        MockServerWebExchange third = run("/biz/register?mobile=13800000000&code=123456", "deviceId", "dev-1");
        assertEquals(HttpStatus.BAD_REQUEST, third.getResponse().getStatusCode());
        assertBody(third, "验证码不存在或已失效");
    }

    @Test
    void mismatchIsRetryable() {
        store.put("13800000000:dev-1:register", code("123456"));
        MockServerWebExchange wrong = run("/biz/register?mobile=13800000000&code=xxxxxx", "deviceId", "dev-1");
        assertEquals(HttpStatus.BAD_REQUEST, wrong.getResponse().getStatusCode());
        assertBody(wrong, "验证码错误");
        // 错误不删码，可重试
        assertEquals(HttpStatus.OK,
                run("/biz/register?mobile=13800000000&code=123456", "deviceId", "dev-1").getResponse().getStatusCode());
    }

    @Test
    void imageCodeCaseInsensitive() {
        store.put("dev-1:login", code("AbCd"));
        assertEquals(HttpStatus.OK,
                run("/biz/login?code=abcd", "deviceId", "dev-1").getResponse().getStatusCode());
    }

    @Test
    void customResolverSuppliesMobile() {
        store.put("17799999999:dev-1:register", code("123456"));
        // 不传 mobile，由自定义方法解析
        assertEquals(HttpStatus.OK,
                run("/biz/custom?code=123456", "deviceId", "dev-1").getResponse().getStatusCode());
    }

    @Test
    void codeHeaderFallback() {
        store.put("dev-1:login", code("AbCd"));
        assertEquals(HttpStatus.OK,
                run("/biz/login", "deviceId", "dev-1", "code", "AbCd").getResponse().getStatusCode());
    }

    private static ValidateCode code(String content) {
        return new ValidateCode(content, Instant.now().plusSeconds(60));
    }
}
