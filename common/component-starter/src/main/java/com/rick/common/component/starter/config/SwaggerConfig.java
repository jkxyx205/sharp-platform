package com.rick.common.component.starter.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Rick.Xu
 * @date 2025/11/13 13:38
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi materialApi() {
        return GroupedOpenApi.builder()
                .displayName("物料 API")
                .group("material-api")                  // 分组名称
                .packagesToScan("com.devyean.erp.module.data.material.controller") // 扫描包
                .pathsToMatch("/materials/**")         // 可选，按路径匹配
                .build();
    }

    @Bean
    public GroupedOpenApi stockApi() {
        return GroupedOpenApi.builder()
                .displayName("库存 API")
                .group("stock-api")                  // 分组名称
                .packagesToScan("com.devyean.erp.module.inventory.controller") // 扫描包
                .pathsToMatch("/stocks/**")         // 可选，按路径匹配
                .build();
    }

}