package com.rick.gateway.controller;

import com.rick.gateway.security.TokenStore;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录/注册入口：凭证校验委托给 platform（lb://platform），token 签发保留在网关。
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    public record LoginRequest(String mobile, String password) {
    }

    public record RegisterRequest(String nickname, String mobile, String password, Character sex) {
    }

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final WebClient webClient;
    private final TokenStore tokenStore;

    // @LoadBalanced 同时作为限定符，匹配 WebClientConfig 中带此注解的 builder
    public AuthController(@LoadBalanced WebClient.Builder webClientBuilder, TokenStore tokenStore) {
        this.webClient = webClientBuilder.build();
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, Object>>> login(@RequestBody LoginRequest request) {
        return webClient.post()
                .uri("lb://platform/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::platformError)
                .bodyToMono(MAP_TYPE)
                .map(user -> {
                    // principal 用 mobile（唯一登录标识），TokenStore 记录 token -> mobile
                    String mobile = user.get("mobile") == null
                            ? request.mobile() : String.valueOf(user.get("mobile"));
                    String token = tokenStore.create(mobile);
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("token", token);
                    body.put("user", user);
                    return ResponseEntity.ok(body);
                })
                .onErrorResume(PlatformErrorException.class, e -> Mono.just(e.toResponse()))
                // 连接失败 / Nacos 无可用实例等基础设施错误
                .onErrorResume(e -> Mono.just(unavailable()));
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, Object>>> register(@RequestBody RegisterRequest request) {
        return webClient.post()
                .uri("lb://platform/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::platformError)
                .bodyToMono(MAP_TYPE)
                .map(ResponseEntity::ok) // 200 + 用户信息，原样透传
                .onErrorResume(PlatformErrorException.class, e -> Mono.just(e.toResponse()))
                .onErrorResume(e -> Mono.just(unavailable()));
    }

    /** platform 返回非 2xx：连同状态码与 JSON body 包装成异常，由 onErrorResume 透传 */
    private Mono<PlatformErrorException> platformError(ClientResponse response) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(MAP_TYPE)
                .defaultIfEmpty(Map.of("code", String.valueOf(status.value()), "message", "认证服务异常"))
                .map(body -> new PlatformErrorException(status, body));
    }

    private static ResponseEntity<Map<String, Object>> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", "503", "message", "用户服务暂不可用"));
    }

    /** platform 侧业务错误（401/403/409 等）：纯控制流，不需要堆栈 */
    private static final class PlatformErrorException extends RuntimeException {

        private final HttpStatusCode status;
        private final Map<String, Object> body;

        private PlatformErrorException(HttpStatusCode status, Map<String, Object> body) {
            super(null, null, false, false);
            this.status = status;
            this.body = body;
        }

        private ResponseEntity<Map<String, Object>> toResponse() {
            return ResponseEntity.status(status).body(body);
        }
    }
}
