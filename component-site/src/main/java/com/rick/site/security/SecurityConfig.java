package com.rick.site.security;

import com.rick.common.component.starter.security.ComponentSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
public class SecurityConfig extends ComponentSecurityConfig {

    @Bean
    @Override
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(new UserHeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return super.securityFilterChain(http);
    }
}
