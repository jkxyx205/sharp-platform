# component-starter — AI 使用规则

> 本文件是给 AI 编程助手（Claude Code / GPT / Gemini 等）的模块使用规则。
> 详细 API → `API.md`；架构与请求生命周期 → `ARCHITECTURE.md`。

## 1. 模块定位

**解决什么问题**：为 Servlet 栈业务服务（component-site、component-erp）提供开箱即用的四类基础能力：

1. **用户上下文**：接收网关透传的 `X-User-Id` / `X-User-Mobile` 请求头，写入 ThreadLocal，Controller 可直接声明 `User` 参数注入
2. **多租户数据库访问**：`TableDAO` 自动为 insert/update/select 拼接 `group_id`（租户号来自路由前缀 `{groupId}`），无上下文时兜底 `1`
3. **通用 CRUD 控制器**：`BaseApi` / `BaseCodeApi` / `BaseFormController` 泛型基类，继承即获得整套 REST 端点
4. **Dubbo 身份透传**：消费端/提供端 Filter 自动经 attachment 传递用户上下文，跨服务后 `group_id` 隔离依然生效

**适用场景**：新的 Servlet 栈业务服务（走网关、按 `group_id` 多租户隔离、用 sharp-database 做 DAO）。

**不适用场景**：
- WebFlux 服务（如 gateway）——本模块基于 Servlet ThreadLocal，网关禁止引入
- platform 服务——platform **不依赖本模块**，维护自己的一套 `com.rick.platform.config.*` / `module.user.UserContextHolder` 副本；改一边不会影响另一边

## 2. 使用原则

- 本模块**不是 Spring Boot 自动装配**（无 `AutoConfiguration.imports`）。接入必须在启动类显式：
  ```java
  @Import({ComponentConfig.class, WebMvcRegistrationsConfig.class, DatabaseConfig.class})
  ```
  三个都要，缺一个就缺对应能力（见 API.md §6）。
- 获取当前用户：**Controller 用 `User` 参数注入；其他层用 `UserContextHolder.get()`（只读）**。`put`/`remove` 由拦截器和 Dubbo Filter 负责，业务代码禁止调用。
- 数据库访问：**注入 `TableDAO` Bean（已 `@Primary`）或经 `BaseServiceImpl`**。不要自己 new `ExtendTableDAOImpl`，不要手写 `group_id` 条件（会被重复拼接）。
- 实体基类：site/erp 的实体**必须继承 `ComponentBasEntity` / `ComponentBaseCodeEntity` / `ComponentBaseCodeDescriptionEntity`**，不要直接继承 sharp-database 的 `BaseEntity` 系列（否则丢失 `groupId` 字段与保存回填）。
- 写 URL：本模块给**所有** Controller 映射自动加 `{groupId}` 前缀（`BasicErrorController` 除外）。`@RequestMapping("plants")` 的实际路径是 `/{groupId}/plants`。测试或客户端调用时必须带前缀，如 `/1/plants`。
- 抛业务异常：用 `ResourceNotFoundException` 或 sharp-common 的 `BizException` + `ExceptionCodeEnum`，统一由 `ApiExceptionHandler`（`ComponentConfig` 自动扫描注册）转 JSON，不要自己 try-catch 转响应。

## 3. 源码阅读规则

```text
默认不要扫描整个模块源码（共 18 个文件，但文档已覆盖全部对外能力）。

使用模块时：
1. 优先阅读本文件（CLAUDE.md）
2. 再阅读 API.md 对应章节
3. 如果 API.md 已能解决问题，不要继续阅读源码
4. 只有在文档无法解决、排查 Bug 或确认特殊行为（标注 [需要确认] 的条目）时，
   才阅读相关源码：
   - 用户上下文/拦截器/参数解析 → config/ComponentConfig.java
   - {groupId} 前缀 → config/WebMvcRegistrationsConfig.java
   - group_id 拼接与回填 → config/DatabaseConfig.java
   - CRUD 端点行为 → controller/BaseApi.java
   - Dubbo 透传 → dubbo/*.java
```

## 4. 文档索引

| 内容 | 位置 |
| --- | --- |
| 详细 API（按功能分类 + 示例 + 常见错误） | `API.md` |
| 架构、请求生命周期、多租户机制 | `ARCHITECTURE.md` |
| 常见错误用法（Common Mistakes） | `API.md` §9 |
| 故障排查速查 | `API.md` §9 各条目的"症状"描述 |

## 5. 禁止行为

- 🚫 不要重新实现用户上下文获取（解析 `X-User-Id` 头、自己建 ThreadLocal）——已有 `UserContextHolder` / `User` 参数注入
- 🚫 不要重新实现 `group_id` 拼接、审计字段填充——`DatabaseConfig` 的 `TableDAO` 已做
- 🚫 不要在业务代码调用 `UserContextHolder.put()/remove()`——生命周期归拦截器/Filter
- 🚫 不要直接实例化或继承 `DatabaseConfig` 里的匿名 `TableDAO`、`ComponentConfig` 里的匿名拦截器/解析器——只通过 `@Import` 使用
- 🚫 不要把 `SqlReportApi` 当作可配置的报表引擎——SQL 映射是硬编码的静态 Map（仅 demo 性质）
- 🚫 不要修改本模块来"顺手修复" platform 的同类代码——platform 用的是自己的副本
- 🚫 不要在 gateway（WebFlux）引入本模块
- 🚫 不要为了理解一个 API 扫描整个模块——先查 `API.md`
- 🚫 `@DubboReference` 不要标在 `final` 字段上（配合 `@FieldDefaults(level = PRIVATE)` 会注入失败；消费端真实代码中已标注此坑，见 erp `PlantController`）
