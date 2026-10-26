package com.rick.demo.config;

import com.rick.common.component.starter.config.ComponentSwaggerConfig;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Rick.Xu
 * @date 2025/11/13 13:38
 */
@Configuration
public class SwaggerConfig extends ComponentSwaggerConfig {

    /**
     * http://localhost:8769/api/demo/swagger-ui/index.html
     * http://localhost:7004/swagger-ui/index.html
     * @return
     */
    @Bean
    public GroupedOpenApi plantApi() {
        return GroupedOpenApi.builder()
                .displayName("Demo API")
                .group("demo-api")                  // 分组名称
                .packagesToScan("com.rick.demo.module.demo.controller") // 扫描包
                .pathsToMatch("/{groupId}/demos/**")         // 可选，按路径匹配
                .build();
    }

}