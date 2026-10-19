package com.rick.site.security;

import com.rick.common.component.starter.config.ComponentConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 从网关透传的 X-User-Id 请求头构建 Spring Security 认证信息（网关已完成 token 认证，
 * 本过滤器只负责把身份转成 Authentication，供方法级 @PreAuthorize 使用）。
 * <p>
 * 权限暂硬编码：userId=1 → plant:current_data（与网关 TokenStore 的 userId=1 → admin 硬编码同性质，
 * 后续接入真实权限体系时一并替换）。
 * <p>
 * 头缺失时不设置认证（保持匿名），不阻断请求——与 starter ComponentConfig 拦截器的宽松策略一致；
 * 匿名请求访问 @PreAuthorize 端点由方法级鉴权拒绝（403）。
 */
public class UserHeaderAuthenticationFilter extends OncePerRequestFilter {

    static final long ADMIN_USER_ID = 1L;
    static final String PERMISSION_PLANT_CURRENT_DATA = "plant:current_data";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String userId = request.getHeader(ComponentConfig.HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            List<GrantedAuthority> authorities = new ArrayList<>();
            if (Long.parseLong(userId) == ADMIN_USER_ID) {
                authorities.add(new SimpleGrantedAuthority(PERMISSION_PLANT_CURRENT_DATA));
            }
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(userId, null, authorities));
        }
        filterChain.doFilter(request, response);
    }
}
