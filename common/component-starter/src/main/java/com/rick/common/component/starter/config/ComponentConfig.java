package com.rick.common.component.starter.config;

import com.rick.common.component.starter.model.User;
import com.rick.common.component.starter.model.UserContextHolder;
import com.rick.common.http.exception.ApiExceptionHandler;
import com.rick.common.http.web.SharpWebMvcConfigurer;
import com.rick.db.repository.support.IdToEntityConverterFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;
import java.util.Map;

/**
 * @author Rick.Xu
 * @date 2025/11/10 16:48
 */
@Configuration
@ComponentScan(basePackageClasses = {ApiExceptionHandler.class})
public class ComponentConfig extends SharpWebMvcConfigurer {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_MOBILE = "X-User-Mobile";

    @Override
    public List<ConverterFactory> converterFactories() {
        // 发起 GET 请求的时候，允许值映射到实体对象的 id 字段上，不常用，提供传参的多样性
        // private Person person;
        // GET person = "1" => person.setId(1L)
        return List.of(new IdToEntityConverterFactory());
    }

    /**
     * 拦截器读取网关透传的 X-User-Id / X-User-Mobile 请求头，
     * 并解析路由前缀 /{groupId} 中的租户号，
     * 放入 UserContextHolder（请求结束清理）。头缺失时不阻断请求。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                String userId = request.getHeader(HEADER_USER_ID);
                if (userId != null && !userId.isBlank()) {
                    User user = new User();
                    user.setId(Long.parseLong(userId));
                    user.setMobile(request.getHeader(HEADER_USER_MOBILE));
                    // 路由统一带 {groupId} 前缀，此时映射已完成，路径变量已放入请求属性
                    @SuppressWarnings("unchecked")
                    Map<String, String> pathVariables = (Map<String, String>)
                            request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
                    String groupId = pathVariables == null ? null : pathVariables.get("groupId");
                    if (groupId != null && !groupId.isBlank()) {
                        user.setGroupId(Long.parseLong(groupId));
                    }
                    UserContextHolder.put(user);
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

    /**
     * Controller 方法可直接声明 User 参数，注入当前登录用户。
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        super.addArgumentResolvers(resolvers); // 父类注册了自己的解析器，不能丢
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
