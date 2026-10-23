package com.rick.site.config;

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
     * http://localhost:8769/api/site/swagger-ui/index.html
     * http://localhost:7001/swagger-ui/index.html
     * @return
     */
    @Bean
    public GroupedOpenApi dogApi() {
        return GroupedOpenApi.builder()
                .displayName("狗 API")
                .group("dog-api")                  // 分组名称
                .packagesToScan("com.rick.site.module.dog.controller") // 扫描包
                .pathsToMatch("/{groupId}/dogs/**")         // 可选，按路径匹配
                .build();
    }

}