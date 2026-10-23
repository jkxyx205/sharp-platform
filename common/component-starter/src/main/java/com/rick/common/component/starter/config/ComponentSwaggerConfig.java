package com.rick.common.component.starter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;

/**
 * 组件服务添加 Swagger 步骤：
 * 1. 创建 SwaggerConfig 并继承 ComponentSwaggerConfig
 *      public class SwaggerConfig extends ComponentSwaggerConfig {}
 * 2. application.yml 添加配置 forward-headers-strategy
 *  server:
 *   # 经网关转发时接收 X-Forwarded-Prefix（/api/erp），springdoc 据此生成带前缀的
 *   # swagger-config/api-docs URL；直连 7002 无该头，不受影响
 *   forward-headers-strategy: framework
 * 3. 网关侧前提（gateway application.yml，全局只需配置一次）：
 *    spring.cloud.gateway.server.webflux.trusted-proxies 必须非空。
 *    Gateway 4.2+ 安全加固：未配置时 XForwardedHeadersFilter 不注册（入站
 *    X-Forwarded-* 还会被移除），下游收不到 X-Forwarded-Prefix，springdoc 生成的
 *    swagger URL 缺 /api/xxx 前缀，经网关的 swagger-ui 将无法加载 API 定义
 * 4. 网关侧放行文档路径（gateway SecurityConfig permitAll，需排在业务路由鉴权规则之前）：
 *    /api/{service}/swagger-ui.html、/api/{service}/swagger-ui/**、/api/{service}/v3/api-docs/**
 * @author Rick.Xu
 * @date 2026/10/23 17:37
 */
public class ComponentSwaggerConfig {

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
     * {groupId} 是 starter WebMvcRegistrationsConfig 统一注入的路由前缀，
     * 不在控制器方法签名里，springdoc 不会为它生成 path 参数——
     * swagger-ui try-it-out 会把 {groupId} 当字面路径原样请求。
     * 这里补充声明为必填 path 参数（租户组 ID，long，本地数据默认 1）。
     */
    @Bean
    public GlobalOpenApiCustomizer groupIdParameterCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, pathItem) -> {
                if (!path.contains("{groupId}")) {
                    return;
                }
                pathItem.readOperations().forEach(operation -> {
                    boolean declared = operation.getParameters() != null && operation.getParameters().stream()
                            .anyMatch(p -> "groupId".equals(p.getName()) && "path".equals(p.getIn()));
                    if (declared) {
                        return;
                    }
                    Parameter groupId = new Parameter()
                            .name("groupId")
                            .in("path")
                            .required(true)
                            .description("租户组 ID")
                            .schema(new IntegerSchema().format("int64")._default(1L));
                    if (operation.getParameters() == null) {
                        operation.setParameters(new ArrayList<>());
                    }
                    // 插到最前，与路径中的顺序一致
                    operation.getParameters().add(0, groupId);
                });
            });
        };
    }
}
