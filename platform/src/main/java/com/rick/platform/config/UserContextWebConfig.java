package com.rick.platform.config;

import com.rick.platform.module.user.UserContextHolder;
import com.rick.platform.module.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 用户上下文：拦截器读取网关透传的 X-User-Id / X-User-Mobile 请求头
 * 放入 UserContextHolder（请求结束清理）；Controller 可直接声明 User 参数注入。
 * 请求头缺失时不阻断请求（匿名接口 /auth/** 无用户上下文）。
 */
@Configuration
public class UserContextWebConfig implements WebMvcConfigurer {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_MOBILE = "X-User-Mobile";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String userId = request.getHeader(HEADER_USER_ID);
                if (userId != null && !userId.isBlank()) {
                    UserContextHolder.put(User.builder()
                            .id(Long.parseLong(userId))
                            .mobile(request.getHeader(HEADER_USER_MOBILE))
                            .build());
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                        Object handler, @Nullable Exception ex) {
                UserContextHolder.remove();
            }
        });
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return User.class.isAssignableFrom(parameter.getParameterType());
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return UserContextHolder.get();
            }
        });
    }
}
