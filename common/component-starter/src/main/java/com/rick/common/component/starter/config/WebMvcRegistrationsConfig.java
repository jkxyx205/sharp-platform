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

                return handlerType == BasicErrorController.class ? info : root.combine(info);
            }
        };
    }
}
