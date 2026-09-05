package com.rick.gateway.config;

import com.rick.gateway.security.ApiTokenAuthentication;
import com.rick.gateway.security.TokenAuthenticationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
public class SecurityConfig {

    private static final String BEARER_PREFIX = "Bearer ";

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http,
                                                      TokenAuthenticationManager tokenAuthenticationManager) {
        ServerAuthenticationEntryPoint unauthorizedEntryPoint = (exchange, e) -> writeJson(
                exchange, HttpStatus.UNAUTHORIZED, "{\"code\":401,\"message\":\"unauthorized\"}");

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/auth/login", "/auth/register", "/sms/{mobile}/register").permitAll()
                        .anyExchange().authenticated())
                .addFilterAt(bearerTokenAuthenticationFilter(tokenAuthenticationManager, unauthorizedEntryPoint),
                        SecurityWebFiltersOrder.AUTHENTICATION)
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
     * 从 Authorization: Bearer xxx 请求头中提取 token。
     */
    private ServerAuthenticationConverter bearerTokenConverter() {
        return exchange -> Mono
                .justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.startsWith(BEARER_PREFIX))
                .map(header -> new ApiTokenAuthentication(header.substring(BEARER_PREFIX.length())));
    }

    private static Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String body) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
