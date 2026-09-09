# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建与测试

项目**没有 gradlew 脚本**（只有 `gradle/wrapper/` 目录），使用系统 Gradle 8.6（已在 PATH）。
系统默认 Java 是 1.8，而工具链要求 17（`gradle/libs.versions.toml` 的 `java-language-version`），
**所有 gradle 命令必须显式指定 JDK 17**：

```bash
export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"  # JBR 17

gradle :gateway:compileJava :platform:compileJava        # 编译
gradle :gateway:test                                      # 模块全量测试
gradle :gateway:test --tests "com.rick.gateway.captcha.ValidateCodeServiceTest"            # 单个测试类
gradle :gateway:test --tests "com.rick.gateway.captcha.ValidateCodeServiceTest.handlerFalseRejectsWithDefaultMessageAndSendsNothing"  # 单个方法
```

测试环境限制（失败前先排除环境因素）：
- platform 的 `TableGeneratorTest` 需要真实 PostgreSQL（执行 CREATE TABLE），无库必失败
- gateway 的 `SmsTest` 是 `@Disabled` 手动冒烟用例，需要 `.env.sms.properties` 里的真实阿里云短信 AK
- gateway 的 `ValidateCodeIntegrationTest` 自足（mock Sender、属性覆盖 captcha 配置、禁用 Nacos discovery）

## 环境与机密

- 机密一律走 gitignored 的 `.env.*.properties`（`.gitignore` 含 `.env*`），由各服务
  `spring.config.import: optional:classpath:.env.xxx.properties` 加载，注入占位符：
  `JDBC.URL/USERNAME/PASSWORD`（postgres）、`ID/SECRET`（阿里云）、`SIGN_NAME`（短信签名）。
  **入库的 yml/properties 只允许 `${...}` 占位符，不允许出现明文值。**
- 本地基础设施：Nacos `127.0.0.1:8848`、PostgreSQL。
- 端口约定：gateway 8769，site 7001，erp 7002，platform 7003；Dubbo 协议 site 20880 / erp 20881 / platform 20882，qos 22223–22225。
- Nacos 分组：Web 实例注册到 `WEB_GROUP`，Dubbo 实例在 `DEFAULT_GROUP`，二者隔离。

## 模块结构

Gradle 多模块（root `sharp-platform`），Spring Boot 3.5.7 / Spring Cloud 2025.0.3 / Dubbo 3.3.0：

- **gateway** — WebFlux + Spring Cloud Gateway，系统唯一入口，无数据库
- **platform** — Servlet 栈业务服务（`module/user`、`module/message`），Dubbo 提供方
- **component-site / component-erp** — Servlet 栈业务服务（`common/` 目录下，settings.gradle 映射）
- **common/component-starter** — 共享库：`DatabaseConfig`（TableDAO 拦截，SQL 自动拼 `group_id`，无用户上下文兜底 1）、`UserContextHolder` + HTTP 拦截器/Dubbo Filter、`Base*Api` 通用控制器
- **common/component-api** — 跨服务 Dubbo API 接口（纯接口库，`bootJar` 已禁用）
- 外部快照库（mavenLocal `~/.m2`）：`com.rick.db:sharp-database`（`BaseServiceImpl`/`TableDAO`，DAO 层基类）、`com.rick.sms:sharp-sms`（短信发送）

依赖规则：**gateway 对外只走 HTTP；Dubbo 仅用于后端服务之间**（如 erp → platform）。gateway 调 platform 用 `@LoadBalanced WebClient`（`lb://platform`，builder 定义在 `gateway/config/WebClientConfig`）。

## 认证与身份链路（跨多文件的核心机制）

1. **网关无状态 token 认证**：登录/注册由 `gateway/controller/AuthController` 委托 platform 校验凭证，token 由网关 `TokenStore`（内存 token → userId/mobile/permissions）签发；注册成功即自动登录，返回 `{token, user}`。权限暂硬编码：userId=1 → `admin`。
2. **认证过滤器**：`SecurityConfig` 手工组装 `AuthenticationWebFilter`，token 提取优先 `Authorization: Bearer`，回退 `access_token` 查询参数（SockJS/WebSocket 握手无法自定义请求头）。未认证/无权限返回 **JSON 401/403，不重定向**（面向前后端分离）。
3. **身份透传**：`UserHeaderGlobalFilter` 在路由转发前以 `set`（覆盖写，防伪造）注入 `X-User-Id` / `X-User-Mobile`；下游服务由拦截器（platform `UserContextWebConfig`、starter `ComponentConfig`）写入 `UserContextHolder`；Dubbo 链路由 `UserContextConsumerFilter`/`UserContextProviderFilter` 经 attachment 传递。数据库访问自动按上下文拼 `group_id`。

**WebFlux 关键约束**（gateway 内）：
- `SecurityContextHolder`（ThreadLocal）永远为 null，用 `ReactiveSecurityContextHolder.getContext()`（必须在响应式链内）或 Controller 参数注入 `Principal`；Service 层优先由调用方把 principal 当参数传入
- 阻塞调用（短信发送、`WebClient...block()`、Dubbo）必须调度到 `Schedulers.boundedElastic()`

## 验证码子系统（gateway/captcha 包）

配置驱动，两个扩展点均按 **Spring Bean 名** 在 yml 引用，启动期 fail-fast 校验 bean 存在（笔误直接启动失败）：

- `captcha.types.{type}.pre-handler` → `CodePreHandler`：发码前业务预检查（如 `registerCodePreHandler` 经 HTTP 查 platform `/auth/mobile_exists`，已注册抛 `PreHandlerException` → 400）
- `captcha.rules[].mobile` → `MobileResolver#getMobile(user, queryMobile, type)`：校验期自定义手机号解析；解析顺序 resolver → token 用户上下文 → query 参数兜底

流程：发送走 `SmsController`/`ImageCodeController` → `ValidateCodeService.sendCode`（内存 `ValidateCodeStore`，key = `mobile:deviceId:type` 或 `deviceId:type`）；校验走 `ValidateCodeFilter`，**下游业务返回 2xx 才消费验证码**，失败保留可重试。

⚠️ `ValidateCodeFilter` **不能注册为 Spring Bean**（WebFlux 会把所有 `WebFilter` Bean 挂进全局过滤链，导致在 security 链之外重复执行）——由 `SecurityConfig` 手工实例化并加在 `AUTHORIZATION` 之后，保证未认证先 401、验证码错误才是 400。

## 路由与 CORS

- 路由：`/api/site/**` → `lb://site`、`/api/erp/**` → `lb://erp`（需 `admin` 权限）、`/api/platform/**` → `lb://platform`，均 `StripPrefix=2`
- yml 的 `globalcors` 只作用于路由转发；网关本地 Controller（如 `/auth/login`）的预检由 `SecurityConfig#corsConfigurationSource` 复用 `GlobalCorsProperties` 交给 `CorsWebFilter` 统一处理，两处配置同源

## 测试约定

- 单元测试为纯 JUnit 5 + Mockito（不起 Spring 上下文），构造器直接 new 被测类；Spring 上下文只在 `ValidateCodeIntegrationTest`（RANDOM_PORT + `@MockitoBean Sender` + 属性覆盖）出现
- 需要 `Map<String, T>` 注入的组件（`ValidateCodeService` 的 preHandlers、`ValidateCodeFilter` 的 resolvers）在测试里直接传 `Map.of(...)` 或 stub `ApplicationContext.getBeansOfType`
