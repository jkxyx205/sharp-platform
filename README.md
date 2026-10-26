# sharp-platform

基于 Spring Boot 3 / Spring Cloud Gateway / Dubbo 3 的微服务平台。网关是系统唯一入口（无状态 token 认证 + 验证码子系统），业务服务之间通过 Dubbo 通信，网关对外只走 HTTP。

## 架构

```
                        ┌─────────────────────────────────────────┐
  Browser / App ──HTTP──▶  gateway (8769, WebFlux + SCG)          │
                        │  · TokenStore（内存 / Redis 可选）         │
                        │  · AuthenticationWebFilter（401/403 JSON）│
                        │  · 验证码子系统（短信 / 图形码，内存 / Redis 可选）│
                        │  · UserHeaderGlobalFilter 注入身份头      │
                        └───────┬──────────────┬──────────────┬───┘
                       /api/site/**    /api/erp/**      /api/platform/**
                                │              │              │
                        ┌───────▼──────┐ ┌─────▼────────┐ ┌───▼────────────┐
                        │ component-   │ │ component-   │ │ platform       │
                        │ site (7001)  │ │ erp (7002)   │ │ (7003)         │
                        └──────────────┘ └──────┬───────┘ └───▲────────────┘
                                          Dubbo │      Dubbo  │
                                                └─────────────┘
                                          （仅后端服务之间，如 erp → platform）
```

- 服务注册：Nacos（`127.0.0.1:8848`），Web 实例在 `WEB_GROUP`，Dubbo 实例在 `DEFAULT_GROUP`
- 数据库：PostgreSQL，DAO 层自动按用户上下文拼接 `group_id` 实现多租户隔离
- 存储：网关 token / 验证码默认内存存储，可切换 Redis（多实例共享、重启不丢失）

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `gateway` | 系统唯一入口，WebFlux + Spring Cloud Gateway，无数据库 |
| `platform` | Servlet 栈业务服务（`module/user`、`module/message`），Dubbo 提供方 |
| `component-site` | Servlet 栈业务服务（站点） |
| `component-erp` | Servlet 栈业务服务（ERP） |
| `component-demo` | 新服务脚手架示例（Demo CRUD），复制它来「添加新的组件服务」 |
| `common/component-starter` | 共享库：`DatabaseConfig`（TableDAO 拦截，SQL 自动拼 `group_id`）、`UserContextHolder` + HTTP 拦截器 / Dubbo Filter、`Base*Api` 通用控制器 |
| `common/component-api` | 跨服务 Dubbo API 接口（纯接口库，`bootJar` 已禁用） |

外部快照库（mavenLocal `~/.m2`）：

- `com.rick.db:sharp-database` — `BaseServiceImpl` / `TableDAO`，DAO 层基类
- `com.rick.sms:sharp-sms` — 阿里云短信发送

## 技术栈

- Java 17
- Spring Boot 3.5.7 / Spring Cloud 2025.0.3
- Apache Dubbo 3.3.0
- Nacos（注册中心）
- PostgreSQL
- Redis（可选，网关 token / 验证码存储后端）
- Gradle 8.6（多模块，version catalog `gradle/libs.versions.toml`）

## 快速开始

### 环境要求

- JDK 17（系统默认 Java 为 1.8 时必须显式指定 `JAVA_HOME`）
- Gradle 8.6（项目无 `gradlew` 脚本，使用系统 Gradle）
- Nacos `127.0.0.1:8848`
- PostgreSQL
- Redis（仅 `token.store=redis` 或 `captcha.store=redis` 时需要）
- mavenLocal 中的 `sharp-database`、`sharp-sms` 快照依赖

### 机密配置

机密一律走 gitignored 的 `.env.*.properties`（`.gitignore` 含 `.env*`），由各服务通过
`spring.config.import: optional:classpath:.env.xxx.properties` 加载。入库的 yml/properties 只允许 `${...}` 占位符，不允许出现明文值。

需提供的占位符：

| 占位符 | 说明 |
| --- | --- |
| `JDBC.URL` / `JDBC.USERNAME` / `JDBC.PASSWORD` | PostgreSQL 连接 |
| `ID` / `SECRET` | 阿里云 AccessKey |
| `SIGN_NAME` | 短信签名 |

### 构建

```bash
export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"  # JDK 17

gradle :gateway:compileJava :platform:compileJava   # 编译
gradle :gateway:test                                 # 测试
```

### 端口约定

| 服务 | HTTP 端口 | Dubbo 端口 | QoS 端口 |
| --- | --- |----------| --- |
| gateway | 8769 | —        | — |
| site | 7001 | 20880    | 22222 |
| erp | 7002 | 20881    | 22223 |
| demo | 7004 | 20883    | 22224 |
| platform | 7003 | 20882    | 22225 |

## 添加新的组件服务

`component-demo` 是一个可运行的最小脚手架（Demo 实体 CRUD 链路 + 全部必备配置），新增服务的推荐方式是**整体复制 component-demo 后改名**。下文 `{name}` 为小写服务名（如 `demo`），`{Name}` 为类名前缀（如 `Demo`）；改动涉及新模块本身、`settings.gradle`、网关三处。

### 1. 模块骨架

```
component-{name}/
├── build.gradle
└── src
    ├── main
    │   ├── java/com/rick/{name}/
    │   │   ├── {Name}Application.java        # 启动类
    │   │   ├── config/SwaggerConfig.java     # springdoc 分组
    │   │   ├── security/                     # SecurityConfig + UserHeaderAuthenticationFilter
    │   │   └── module/
    │   │       ├── common/exception/ExceptionCodeEnum.java   # 业务错误码（implements ExceptionCode）
    │   │       └── {module}/                 # entity / dao / service / controller
    │   └── resources/
    │       ├── application.yml               # 各环境共用配置（无机密）
    │       ├── application-dev.yml           # 本地环境（数据源占位符、Nacos、Dubbo 端口）
    │       └── .env.postgres.properties      # gitignored 机密（JDBC.*）
    └── test/java/com/rick/{name}/            # BaseTest / TestHelper / 建表与代码生成测试
```

`build.gradle` 只声明坐标与内部依赖，插件（Spring Boot / 依赖管理 / Java 17 toolchain / Lombok / maven-publish）由根 `build.gradle` 的 `subprojects` 统一应用：

```groovy
group = 'com.rick.{name}'
version = libs.versions.sharp.get()
description = '{name}'

dependencies {
    implementation project(':component-starter')
    implementation project(':component-api')
}
```

然后在 `settings.gradle` 的 include 列表加上模块名（root 直下模块无需 `projectDir` 映射）：

```groovy
include "component-starter", "component-api", "component-demo", "component-site", "component-erp", "gateway", "platform"
```

### 2. 启动类与通用配置

`{Name}Application`（与 `DemoApplication` 同款）：

```java
@SpringBootApplication
@DependsOn("entityDAOSupport")
@Import({DocumentDAO.class, DocumentServiceImpl.class, DocumentController.class,
        ComponentConfig.class, WebMvcRegistrationsConfig.class, DatabaseConfig.class})
@EnableDubbo
public class DemoApplication { ... }
```

- starter 三配置：`ComponentConfig`（身份头 → `UserContextHolder`）、`WebMvcRegistrationsConfig`（业务 Controller 统一加 `{groupId}` 路径变量前缀，springdoc 端点除外）、`DatabaseConfig`（TableDAO 拦截，SQL 自动拼 `group_id`）
- `Document*` 为 fileupload 文档上传下载支持类
- `security` 包两个类：`SecurityConfig extends ComponentSecurityConfig` 把 `UserHeaderAuthenticationFilter` 加在 `UsernamePasswordAuthenticationFilter` 之前；filter 把网关透传的 `X-User-Id` 转成 `Authentication` 供 `@PreAuthorize` 方法级鉴权（头缺失保持匿名不阻断，权限暂硬编码，与网关 TokenStore 同性质）
- `SwaggerConfig extends ComponentSwaggerConfig`，声明 `GroupedOpenApi`（`packagesToScan` 指向本模块 controller 包）

两个 yml 的关键项（其余照抄 demo）：

| 文件 | 配置 | 说明 |
| --- | --- | --- |
| application.yml | `spring.application.name: {name}` | Nacos 服务名，即网关 `lb://{name}` 的路由目标 |
| | `sharp.database.entity-base-package: com.rick.{name}.**.entity` | 实体扫描包 |
| | `server.port` | 取端口约定表的下一个空闲端口（demo = 7004） |
| | `server.forward-headers-strategy: framework` | 经网关转发时 springdoc 依 `X-Forwarded-Prefix` 生成带 `/api/{name}` 前缀的文档 URL |
| application-dev.yml | `spring.datasource.*: ${JDBC.*}` | 真实值放 gitignored 的 `.env.postgres.properties` |
| | `spring.cloud.nacos.discovery.group: WEB_GROUP` | Web 实例与 Dubbo 实例（DEFAULT_GROUP）隔离 |
| | `dubbo.protocol.port` / `dubbo.application.qos-port` | ⚠️ 必须与所有既有服务错开（见端口约定表） |

### 3. 业务模块代码

`module/{module}/` 下四层链路，加上 `module/common/exception/ExceptionCodeEnum`（业务错误码），即 sharp-entity skill 所称「五件套」。无 code/description 字段的简单实体用下列基类（与 `Demo` 同款）：

| 层 | 写法 |
| --- | --- |
| entity | `@Table(value = "t_demo", comment = "demo")`，extends `ComponentBaseEntity<Long>`（基类自带 id / 审计字段 / groupId） |
| dao | `@Repository`，extends `EntityDAOImpl<Demo, Long>` |
| service | `@Service @Validated`，extends `BaseServiceImpl<DemoDAO, Demo, Long>`，构造器注入 DAO |
| controller | `@RestController @RequestMapping("demos")`，extends `BaseApi<DemoService, Demo, Long>` |

带 code/description 字段的基类变体见 `.claude/skills/sharp-entity` skill；也可用 `CodeGeneratorTest` 的 `Generator.execute(实体类, 目标目录, Map.of(Generator.PROJECT, "component-{name}"))` 直接生成整套。

### 4. 网关接入（gateway 两处）

`gateway/src/main/resources/application.yml` 的 `routes` 增加一条：

```yaml
- id: demo
  uri: lb://demo
  predicates:
    - Path=/api/demo/**
  filters:
    - StripPrefix=2
```

`StripPrefix=2` 剥掉 `/api/demo`，叠加 `{groupId}` 前缀后，对外路径即 `/api/demo/{groupId}/demos/**`。

再到 `gateway/config/SecurityConfig` 的 permitAll 列表放行文档路径：

```java
"/api/demo/swagger-ui.html", "/api/demo/swagger-ui/**", "/api/demo/v3/api-docs/**",
```

业务路径默认被 `anyExchange().authenticated()` 覆盖（登录即可访问）；需要特定权限时参照 `/api/erp/**` 增加 `.pathMatchers(...).hasAuthority(...)`。

### 5. 建表、启动与验证

```bash
export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"  # JDK 17

gradle :component-demo:compileJava    # 编译通过
# 建表：需要真实 PostgreSQL；createTable 非幂等（表已存在会失败），用单方法执行
gradle :component-demo:test --tests "com.rick.demo.TableGeneratorTest.testGeneratorDemoTable"
gradle :component-demo:bootRun        # 启动，注册到 Nacos WEB_GROUP
```

先登录拿 token，经网关验证（文档地址：经网关 `http://localhost:8769/api/demo/swagger-ui/index.html`，直连 `http://localhost:7004/swagger-ui/index.html`）：

```bash
curl -X POST 'http://localhost:8769/api/demo/999/demos' \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer {token}' \
  -d '{"name": "demo"}'
```

测试约定与其他模块一致：`BaseTest` 提供 CRUD 生命周期集成测试基类（`@SpringBootTest`，需真实 PostgreSQL）；`TableGeneratorTest` / `CodeGeneratorTest` 自足运行（随机 Dubbo 端口 + 不注册 Nacos，防止网关把流量路由到测试进程）。

## 认证与身份链路

1. **登录 / 注册**：`gateway/controller/AuthController` 委托 platform 校验凭证，token 由网关 `TokenStore`（token → userId / mobile / permissions）签发；注册成功即自动登录，返回 `{token, user}`。权限暂硬编码：userId=1 → `admin`。
2. **认证过滤器**：`SecurityConfig` 手工组装 `AuthenticationWebFilter`，token 提取优先 `Authorization: Bearer`，回退 `access_token` 查询参数（兼容 SockJS/WebSocket 握手）。未认证 / 无权限返回 **JSON 401/403，不重定向**。
3. **身份透传**：`UserHeaderGlobalFilter` 在路由转发前以覆盖写方式注入 `X-User-Id` / `X-User-Mobile`（防伪造）；下游服务由拦截器写入 `UserContextHolder`；Dubbo 链路由 `UserContextConsumerFilter` / `UserContextProviderFilter` 经 attachment 传递。数据库访问自动按上下文拼 `group_id`。

### 网关 API

| 端点 | 说明 |
| --- | --- |
| `POST /auth/login` | 账号密码登录 |
| `POST /auth/mobile_login` | 手机验证码登录 |
| `POST /auth/register` | 注册（成功即自动登录） |
| `POST /auth/logout` | 登出 |
| `GET /sms/{mobile}/register` | 发送注册验证码 |
| `GET /sms/{mobile}/mobile_login` | 发送登录验证码 |
| `GET /image/login` 等 | 图形验证码 |
| `/api/demo/**` → `lb://demo` | 示例服务（StripPrefix=2） |
| `/api/site/**` → `lb://site` | 站点服务（StripPrefix=2） |
| `/api/erp/**` → `lb://erp` | ERP 服务（需 `admin` 权限） |
| `/api/platform/**` → `lb://platform` | 平台服务 |

## 验证码子系统（gateway/captcha）

配置驱动，两个扩展点均按 **Spring Bean 名** 在 yml 引用，启动期 fail-fast 校验 bean 存在：

- `captcha.types.{type}.pre-handler` → `CodePreHandler`：发码前业务预检查（如 `registerCodePreHandler` 经 HTTP 查 platform `/auth/mobile_exists`，已注册抛 `PreHandlerException` → 400）
- `captcha.rules[].mobile` → `MobileResolver#getMobile(user, queryMobile, type)`：校验期自定义手机号解析；解析顺序 resolver → token 用户上下文 → query 参数兜底

流程：发送走 `SmsController` / `ImageCodeController` → `ValidateCodeService.sendCode`（`ValidateCodeStore`，key = `mobile:deviceId:type` 或 `deviceId:type`）；校验走 `ValidateCodeFilter`，**下游业务返回 2xx 才消费验证码**，失败保留可重试。

### 存储后端（内存 / Redis 可选）

`TokenStore` 与 `ValidateCodeStore` 均抽象为接口，由配置 `token.store` / `captcha.store` 选择实现，两者独立：

| 配置 | 取值 | 实现 | 说明 |
| --- | --- | --- | --- |
| `token.store` | `memory`（缺省）/ `redis` | `InMemoryTokenStore` / `RedisTokenStore` | token → userId/mobile/permissions |
| `captcha.store` | `memory`（缺省）/ `redis` | `InMemoryValidateCodeStore` / `RedisValidateCodeStore` | 验证码内容 + 过期时间 |

- 内存实现为单机默认，进程重启即丢失
- Redis 实现以 key 自带 TTL / Hash 存储，多实例共享、重启不丢失；`captcha` 的 `consume` 用 Lua 原子比对删除，防止并发双提交与重发误删
- Redis 连接走 Spring Boot 默认 `spring.data.redis.*` 自动配置

> ⚠️ `ValidateCodeFilter` 不能注册为 Spring Bean（WebFlux 会把所有 `WebFilter` Bean 挂进全局过滤链导致重复执行），由 `SecurityConfig` 手工实例化并加在 `AUTHORIZATION` 之后，保证未认证先 401、验证码错误才是 400。

## CORS

- yml 的 `globalcors` 只作用于路由转发（`/api/**`）
- 网关本地 Controller（如 `/auth/login`）的预检由 `SecurityConfig#corsConfigurationSource` 复用 `GlobalCorsProperties` 交给 `CorsWebFilter` 统一处理，两处配置同源

## 测试

单元测试为纯 JUnit 5 + Mockito（不起 Spring 上下文），构造器直接 new 被测类。Spring 上下文只在 `ValidateCodeIntegrationTest`（RANDOM_PORT + `@MockitoBean Sender` + 属性覆盖）出现。

```bash
gradle :gateway:test                                                                       # 模块全量
gradle :gateway:test --tests "com.rick.gateway.captcha.ValidateCodeServiceTest"            # 单个测试类
gradle :gateway:test --tests "com.rick.gateway.captcha.ValidateCodeServiceTest.handlerFalseRejectsWithDefaultMessageAndSendsNothing"  # 单个方法
```

已知环境限制（失败前先排除环境因素）：

- platform 的 `TableGeneratorTest` 需要真实 PostgreSQL（执行 CREATE TABLE），无库必失败
- gateway 的 `SmsTest` 是 `@Disabled` 手动冒烟用例，需要 `.env.sms.properties` 里的真实阿里云短信 AK
- gateway 的 `ValidateCodeIntegrationTest` 自足（mock Sender、属性覆盖 captcha 配置、禁用 Nacos discovery）
