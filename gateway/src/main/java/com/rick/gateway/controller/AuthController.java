package com.rick.gateway.controller;

import com.rick.gateway.security.TokenResolver;
import com.rick.gateway.security.TokenStore;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * 手机验证码登录
     * @param request
     * @return
     */
    @PostMapping("/mobile_login")
    public Mono<ResponseEntity<Map<String, Object>>> mobileLogin(@RequestBody LoginRequest request) {
        return webClient.post()
                .uri("lb://platform/auth/mobileLogin")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::platformError)
                .bodyToMono(MAP_TYPE)
                .map(user -> issueToken(user, request.mobile()))
                .onErrorResume(PlatformErrorException.class, e -> Mono.just(e.toResponse()))
                // 连接失败 / Nacos 无可用实例等基础设施错误
                .onErrorResume(e -> Mono.just(unavailable()));
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
                .map(user -> issueToken(user, request.mobile()))
                .onErrorResume(PlatformErrorException.class, e -> Mono.just(e.toResponse()))
                // 连接失败 / Nacos 无可用实例等基础设施错误
                .onErrorResume(e -> Mono.just(unavailable()));
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, Object>>> register(@RequestBody RegisterRequest request, @RequestParam String mobile) {
        return webClient.post()
                .uri("lb://platform/auth/register?mobile=" + mobile)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::platformError)
                .bodyToMono(MAP_TYPE)
                // 注册成功即自动登录：签发 token，返回 {token, user}，与登录接口同构
                .map(user -> issueToken(user, mobile))
                .onErrorResume(PlatformErrorException.class, e -> Mono.just(e.toResponse()))
                .onErrorResume(e -> Mono.just(unavailable()));
    }

    /**
     * 签发 token 并组装 {token, user} 响应（登录/注册成功共用）。
     * principal 用 mobile（唯一登录标识），TokenStore 记录 token -> (userId, mobile)。
     */
    private ResponseEntity<Map<String, Object>> issueToken(Map<String, Object> user, String fallbackMobile) {
        String mobile = user.get("mobile") == null
                ? fallbackMobile : String.valueOf(user.get("mobile"));
        Long userId = user.get("id") instanceof Number id ? id.longValue() : null;
        // 权限硬编码：userId=1 视为管理员，后续接入 DB 角色后替换
        List<String> permissions = userId != null && userId == 1L
                ? List.of("user", "admin") : List.of("user");
        String token = tokenStore.create(userId, mobile, permissions);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("user", user);
        return ResponseEntity.ok(body);
    }

    /**
     * 退出登录：移除网关内存中的 token，使其立即失效。
     * 未列入 SecurityConfig 的 permitAll，因此需携带有效 token 才能到达此处。
     */
    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, Object>>> logout(ServerHttpRequest request, Principal principal) {
        System.out.println(principal);
        String token = TokenResolver.resolveToken(request);
        if (token != null) {
            tokenStore.remove(token);
        }
        return Mono.just(ResponseEntity.ok(Map.of("code", "200", "message", "已退出登录")));
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
