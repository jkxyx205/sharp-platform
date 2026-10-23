package com.rick.common.component.starter.config;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * @author Rick
 * @createdAt 2021-10-27 15:26:00
 */
@Configuration
public class WebMvcRegistrationsConfig implements WebMvcRegistrations {
    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {

        return new RequestMappingHandlerMapping() {

            @Override
            protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
                RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

                RequestMappingInfo root = RequestMappingInfo
                        .paths("{groupId}").build();
                if (info == null) {
                    return super.getMappingForMethod(method, handlerType);
                }

                // springdoc 端点（/v3/api-docs、/swagger-ui.html）不加前缀：
                // swagger-config 的分组 URL 由 springdoc.api-docs.path 拼接生成，
                // 加了 {groupId} 前缀会导致生成的 URL 与实际映射不一致（404）
                return handlerType == BasicErrorController.class
                        || handlerType.getPackageName().startsWith("org.springdoc") ? info : root.combine(info);
            }
        };
    }
}
