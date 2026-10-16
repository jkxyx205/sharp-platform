package com.rick.erp.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * erp 方法级鉴权配置（参考 gateway SecurityConfig 的 JSON 响应风格，但 erp 是 Servlet 栈，
 * 用 {@link AuthenticationEntryPoint} / {@link AccessDeniedHandler}，不是 WebFlux 的 Server* 接口）。
 * <p>
 * 认证由网关统一完成（token → X-User-Id 透传），erp 不做登录，因此：
 * 覆盖 spring-boot-starter-security 的默认全端点拦截，filter 级放行所有请求，
 * 鉴权只由 @PreAuthorize（@EnableMethodSecurity 激活）在方法级执行。
 * <p>
 * 无权限时的 JSON 响应分两层：
 * <ul>
 *   <li>@PreAuthorize 拒绝（MVC 内）：抛 AccessDeniedException，由 sharp-common 的
 *       ApiExceptionHandler 转 JSON 403（{"code":403,"message":"访问未授权"}）</li>
 *   <li>到达过滤器链的认证/授权异常：由下面 exceptionHandling 的 entryPoint /
 *       accessDeniedHandler 兜底，返回与网关一致格式的 JSON 401 / 403（默认是空 body）</li>
 * </ul>
 * 无状态会话 + 禁用 csrf：纯 token API，无浏览器会话可保护。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 未认证（匿名）访问受保护资源：返回 JSON 401，与网关 unauthorizedEntryPoint 一致
        AuthenticationEntryPoint unauthorizedEntryPoint = (request, response, e) -> writeJson(
                response, HttpStatus.UNAUTHORIZED, "{\"code\":401,\"message\":\"unauthorized\"}");
        // 已认证但权限不足：返回 JSON 403（默认是空 body），与网关 accessDeniedHandler 一致
        AccessDeniedHandler accessDeniedHandler = (request, response, e) -> writeJson(
                response, HttpStatus.FORBIDDEN, "{\"code\":403,\"message\":\"forbidden\"}");

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new UserHeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorizedEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .build();
    }

    private static void writeJson(HttpServletResponse response, HttpStatus status, String body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(body);
    }
}
