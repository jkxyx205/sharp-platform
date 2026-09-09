# sharp-platform

基于 Spring Boot 3 / Spring Cloud Gateway / Dubbo 3 的微服务平台。网关是系统唯一入口（无状态 token 认证 + 验证码子系统），业务服务之间通过 Dubbo 通信，网关对外只走 HTTP。

## 架构

```
                        ┌─────────────────────────────────────────┐
  Browser / App ──HTTP──▶  gateway (8769, WebFlux + SCG)          │
                        │  · TokenStore（内存 token）              │
                        │  · AuthenticationWebFilter（401/403 JSON）│
                        │  · 验证码子系统（短信 / 图形码）           │
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

## 模块结构

| 模块 | 说明 |
| --- | --- |
| `gateway` | 系统唯一入口，WebFlux + Spring Cloud Gateway，无数据库 |
| `platform` | Servlet 栈业务服务（`module/user`、`module/message`），Dubbo 提供方 |
| `component-site` | Servlet 栈业务服务（站点） |
| `component-erp` | Servlet 栈业务服务（ERP） |
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
- Gradle 8.6（多模块，version catalog `gradle/libs.versions.toml`）

## 快速开始

### 环境要求

- JDK 17（系统默认 Java 为 1.8 时必须显式指定 `JAVA_HOME`）
- Gradle 8.6（项目无 `gradlew` 脚本，使用系统 Gradle）
- Nacos `127.0.0.1:8848`
- PostgreSQL
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
| --- | --- | --- | --- |
| gateway | 8769 | — | — |
| site | 7001 | 20880 | 22223 |
| erp | 7002 | 20881 | 22224 |
| platform | 7003 | 20882 | 22225 |

## 认证与身份链路

1. **登录 / 注册**：`gateway/controller/AuthController` 委托 platform 校验凭证，token 由网关 `TokenStore`（内存 token → userId / mobile / permissions）签发；注册成功即自动登录，返回 `{token, user}`。权限暂硬编码：userId=1 → `admin`。
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
| `/api/site/**` → `lb://site` | 站点服务（StripPrefix=2） |
| `/api/erp/**` → `lb://erp` | ERP 服务（需 `admin` 权限） |
| `/api/platform/**` → `lb://platform` | 平台服务 |

## 验证码子系统（gateway/captcha）

配置驱动，两个扩展点均按 **Spring Bean 名** 在 yml 引用，启动期 fail-fast 校验 bean 存在：

- `captcha.types.{type}.pre-handler` → `CodePreHandler`：发码前业务预检查（如 `registerCodePreHandler` 经 HTTP 查 platform `/auth/mobile_exists`，已注册抛 `PreHandlerException` → 400）
- `captcha.rules[].mobile` → `MobileResolver#getMobile(user, queryMobile, type)`：校验期自定义手机号解析；解析顺序 resolver → token 用户上下文 → query 参数兜底

流程：发送走 `SmsController` / `ImageCodeController` → `ValidateCodeService.sendCode`（内存 `ValidateCodeStore`，key = `mobile:deviceId:type` 或 `deviceId:type`）；校验走 `ValidateCodeFilter`，**下游业务返回 2xx 才消费验证码**，失败保留可重试。

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
