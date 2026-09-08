package com.rick.gateway;

import com.rick.gateway.captcha.ValidateCode;
import com.rick.gateway.captcha.ValidateCodeStore;
import com.rick.gateway.security.TokenStore;
import com.rick.sms.core.Sender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * 端到端：真实 security 过滤链下的图片/短信验证码收发与校验。
 * Sender 被 mock，不会真发短信；测试属性声明专用规则，避免依赖真实下游服务。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "captcha.rules[0].url=/test/image-flow",
        "captcha.rules[0].type=login",
        "captcha.rules[1].url=/test/sms-flow",
        "captcha.rules[1].type=register",
        "captcha.rules[2].url=/test/protected",
        "captcha.rules[2].type=login",
})
class ValidateCodeIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    TokenStore tokenStore;

    @Autowired
    ValidateCodeStore codeStore;

    // 替换 AliSender，ValidateCodeSender 仍为真实 Bean，内部委托该 mock
    @MockitoBean
    Sender sender;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(10)).build();
    }

    /** 直接造 token 绕过登录（用户 mobile 固定 13800000000） */
    private String bearer() {
        return "Bearer " + tokenStore.create(1L, "13800000000", List.of("user"));
    }

    @Test
    void imageFlowEndToEnd() {
        client.get().uri("/image/login").header("deviceId", "dev-img").exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_PNG)
                .expectHeader().cacheControl(CacheControl.noStore())
                .expectBody(byte[].class).value(bytes -> assertThat(bytes.length).isGreaterThan(0));

        String code = codeStore.get("dev-img:login").orElseThrow().content();

        // 正确验证码放行
        client.get().uri(b -> b.path("/test/image-flow").queryParam("code", code).build())
                .header("deviceId", "dev-img").header(HttpHeaders.AUTHORIZATION, bearer())
                .exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("ok");

        // 一次性：二次提交返回已失效
        client.get().uri(b -> b.path("/test/image-flow").queryParam("code", code).build())
                .header("deviceId", "dev-img").header(HttpHeaders.AUTHORIZATION, bearer())
                .exchange().expectStatus().isBadRequest()
                .expectBody(String.class).value(b -> assertThat(b).contains("验证码不存在或已失效"));
    }

    @Test
    void smsFlowEndToEnd() {
        client.get().uri("/sms/13800000000/register").header("deviceId", "dev-sms").exchange()
                .expectStatus().isOk();

        // 按 type 选择模板，参数 code 即存储的码
        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.captor();
        verify(sender).send(eq("13800000000"), eq("xx科技"), eq("SMS_23320004"), params.capture());
        String sent = params.getValue().get("code");
        String stored = codeStore.get("13800000000:dev-sms:register").orElseThrow().content();
        assertThat(sent).isEqualTo(stored).matches("\\d{6}");

        // 认证用户（UserContext mobile 与发送号码一致）携带正确验证码放行
        client.get().uri(b -> b.path("/test/sms-flow").queryParam("code", sent).build())
                .header("deviceId", "dev-sms").header(HttpHeaders.AUTHORIZATION, bearer())
                .exchange().expectStatus().isOk();
    }

    @Test
    void unauthenticatedGets401BeforeCaptchaCheck() {
        client.get().uri("/test/protected").header("deviceId", "dev-x").exchange()
                .expectStatus().isUnauthorized()
                .expectBody(String.class).value(b -> assertThat(b).contains("unauthorized"));
    }

    @Test
    void missingCodeRejected() {
        client.get().uri("/test/image-flow")
                .header("deviceId", "dev-x").header(HttpHeaders.AUTHORIZATION, bearer())
                .exchange().expectStatus().isBadRequest()
                .expectBody(String.class).value(b -> assertThat(b).contains("验证码不能为空"));
    }

    @Test
    void mismatchIsRetryable() {
        client.get().uri("/image/login").header("deviceId", "dev-m").exchange()
                .expectStatus().isOk();
        String code = codeStore.get("dev-m:login").orElseThrow().content();

        client.get().uri(b -> b.path("/test/image-flow").queryParam("code", code + "x").build())
                .header("deviceId", "dev-m").header(HttpHeaders.AUTHORIZATION, bearer())
                .exchange().expectStatus().isBadRequest()
                .expectBody(String.class).value(b -> assertThat(b).contains("验证码错误"));

        client.get().uri(b -> b.path("/test/image-flow").queryParam("code", code).build())
                .header("deviceId", "dev-m").header(HttpHeaders.AUTHORIZATION, bearer())
                .exchange().expectStatus().isOk();
    }

    @Test
    void expiredCodeRejected() {
        codeStore.put("dev-e:login", new ValidateCode("ABCD", Instant.now().minusSeconds(1)));
        client.get().uri(b -> b.path("/test/image-flow").queryParam("code", "ABCD").build())
                .header("deviceId", "dev-e").header(HttpHeaders.AUTHORIZATION, bearer())
                .exchange().expectStatus().isBadRequest()
                .expectBody(String.class).value(b -> assertThat(b).contains("验证码不存在或已失效"));
    }
}
