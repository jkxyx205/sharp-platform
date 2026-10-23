package com.rick.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Rick.Xu
 * @date 2025/11/13 13:38
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME = "bearer";

    /**
     * 全局安全方案：接口经网关访问需认证（token 由网关 TokenStore 签发）。
     * swagger-ui 显示 Authorize 按钮，填入 token 后 try-it-out 请求
     * 自动携带 Authorization: Bearer &lt;token&gt;（与网关 TokenResolver 的提取方式一致）。
     */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("API").version("v1"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("先经网关 POST /auth/login 获取 token，再填入此处")))
                // 全局应用：所有接口带锁图标，Authorize 一次即全部生效
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }

    /**
     * http://localhost:8769/api/platform/swagger-ui/index.html
     * http://localhost:7003/swagger-ui/index.html
     * @return
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .displayName("User API")
                .group("user-api")                  // 分组名称
                .packagesToScan("com.rick.platform.module.user.controller") // 扫描包
                .pathsToMatch("/users/**")         // 可选，按路径匹配
                .build();
    }

}