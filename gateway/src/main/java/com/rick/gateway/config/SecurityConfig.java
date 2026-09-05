package com.rick.gateway.config;

import com.rick.gateway.captcha.ValidateCodeFilter;
import com.rick.gateway.captcha.ValidateCodeProperties;
import com.rick.gateway.captcha.ValidateCodeService;
import com.rick.gateway.security.ApiTokenAuthentication;
import com.rick.gateway.security.TokenAuthenticationManager;
import com.rick.gateway.security.TokenResolver;
import com.rick.gateway.security.TokenStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(ValidateCodeProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http,
                                                      TokenAuthenticationManager tokenAuthenticationManager,
                                                      TokenStore tokenStore,
                                                      ValidateCodeProperties captchaProperties,
                                                      ValidateCodeService validateCodeService,
                                                      ApplicationContext context) {
        ServerAuthenticationEntryPoint unauthorizedEntryPoint = (exchange, e) -> writeJson(
                exchange, HttpStatus.UNAUTHORIZED, "{\"code\":401,\"message\":\"unauthorized\"}");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/login", "/auth/register", "/api/platform/auth/register",
                                "/sms/{mobile}/register", "/sms/{mobile}", "/image/**").permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(bearerTokenAuthenticationFilter(tokenAuthenticationManager, unauthorizedEntryPoint),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                // 验证码校验过滤器：位于授权之后，未认证请求先得到 401，验证码问题才是 400
                .addFilterAfter(new ValidateCodeFilter(captchaProperties, validateCodeService, tokenStore, context),
                        SecurityWebFiltersOrder.AUTHORIZATION)
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedEntryPoint))
                .build();
    }

    private AuthenticationWebFilter bearerTokenAuthenticationFilter(
            TokenAuthenticationManager tokenAuthenticationManager,
            ServerAuthenticationEntryPoint unauthorizedEntryPoint) {
        AuthenticationWebFilter filter = new AuthenticationWebFilter(tokenAuthenticationManager);
        filter.setServerAuthenticationConverter(bearerTokenConverter());
        // token 校验失败时返回 JSON 401，而不是默认的 500
        filter.setAuthenticationFailureHandler(
                new ServerAuthenticationEntryPointFailureHandler(unauthorizedEntryPoint));
        return filter;
    }

    /**
     * 提取 token：优先 Authorization: Bearer xxx 请求头；
     * SockJS/WebSocket 握手无法自定义请求头，回退到 access_token 查询参数。
     */
    private ServerAuthenticationConverter bearerTokenConverter() {
        return exchange -> Mono
                .justOrEmpty(TokenResolver.resolveToken(exchange.getRequest()))
                .map(ApiTokenAuthentication::new);
    }

    private static Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String body) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
