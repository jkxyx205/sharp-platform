# component-starter API

> 面向 AI 编程助手的能力手册。按功能分类，非按 package 排列。
> 分级标记：⭐ 推荐使用 · ⚠️ 特定场景使用 · 🚫 内部实现/不要直接使用
>
> 包根：`com.rick.common.component.starter`
> 消费方：component-site、component-erp（`implementation project(':component-starter')`）。platform **不使用**本模块。

## 目录

1. [获取当前用户](#1-获取当前用户)
2. [多租户数据库访问（TableDAO）](#2-多租户数据库访问tabledao)
3. [基础实体类](#3-基础实体类)
4. [通用 REST 控制器基类](#4-通用-rest-控制器基类)
5. [异常与错误码](#5-异常与错误码)
6. [Web 装配（三个 @Import 配置类）](#6-web-装配三个-import-配置类)
7. [Dubbo 身份透传（自动生效）](#7-dubbo-身份透传自动生效)
8. [传递依赖](#8-传递依赖)
9. [Common Mistakes](#9-common-mistakes)

---

## 1. 获取当前用户

### 1.1 Controller 参数注入 `User` ⭐

**用途**：Controller 方法直接声明 `User` 类型参数，框架注入当前登录用户。

```java
@GetMapping("sites")
public String getSites(User user) {
    // user 来自 UserContextHolder，可能为 null（见下）
    return "userId=" + user.getId();
}
```

**返回值语义**：
- 网关注入了 `X-User-Id` 头 → 返回填充了 `id`/`mobile`/`groupId` 的 `User`
- 请求未经网关认证（本地直调、头缺失）→ **返回 `null`**（拦截器不阻断请求，解析器直接回 `UserContextHolder.get()`）
- `groupId` 仅在路由含 `{groupId}` 前缀变量时才被填充（正常经网关的请求都有）

**适用**：Controller 层获取当前用户。

**不要**：不要从 `HttpServletRequest` 手动读 `X-User-Id` 头再自行 parse。

### 1.2 `UserContextHolder.get()` ⭐（只读）

```java
public static User get()          // 读取，可能返回 null
public static void put(User user) // 🚫 内部：仅拦截器/Dubbo Filter 调用
public static void remove()       // 🚫 内部：仅拦截器/Dubbo Filter 调用
```

**用途**：Service、DAO 回调、任意业务代码获取当前请求用户。

**返回值**：当前线程绑定的 `User`；**无上下文时为 `null`**，使用前判空。

**适用**：
- Service 层获取当前操作人
- Dubbo `@DubboService` 实现内（提供端 Filter 已从 attachment 还原上下文）

**⚠️ ThreadLocal 限制**：
- 只在**请求线程**内有值。`@Async`、自建线程、定时任务中 `get()` 返回 `null`，此时数据库访问兜底 `group_id = 1`（见 §2）
- 仅 Servlet 栈可用；WebFlux（gateway）中永远为 null

**不要**：
- 业务代码不要调用 `put()`/`remove()`——写入与清理由 `ComponentConfig` 拦截器（HTTP）和 `UserContextProviderFilter`（Dubbo）负责，手动 put 会破坏请求结束后的清理契约
- 不要自己解析 token——认证在网关完成，本模块只消费透传头

### 1.3 `User` 模型 ⭐

```java
@Data
public class User {
    Long id;        // 用户 id，可能为 null
    String mobile;  // 手机号，可能为 null
    Long groupId;   // 租户/公司 id，来自路由 {groupId} 前缀，可能为 null
}
```

三个字段**均可为 null**（头缺失或路由无前缀时）。序列化输出中 `groupId` 在实体上是字符串（见 §3），`User` 本身无此处理。

### 1.4 请求头常量 ⭐

```java
ComponentConfig.HEADER_USER_ID     // "X-User-Id"
ComponentConfig.HEADER_USER_MOBILE // "X-User-Mobile"
ComponentConfig.HEADER_GROUP_ID    // "X-Group-Id"（仅 Dubbo attachment 使用）
```

**用途**：写集成测试、模拟网关请求、或实现自定义透传时引用常量，不要硬编码字符串。

---

## 2. 多租户数据库访问（TableDAO）

### 2.1 注入 `TableDAO` ⭐

```java
@Resource
protected TableDAO tableDAO;   // DatabaseConfig 提供，@Primary
```

由 `DatabaseConfig#tableDAO` 提供的 `ExtendTableDAOImpl` 匿名子类（sharp-database 基类），**自动**做以下事情，业务代码零感知：

| 操作 | 自动行为 |
| --- | --- |
| insert（经 `addInsertInfo`） | 参数 map 自动放入 `group_id`（当前上下文，无则 `1L`） |
| `update(tableName, columnsCondition, condition, Map/Object...)` 两个重载 | 条件自动追加 `AND group_id = :group_id`（或 `= ?`），参数自动补齐 |
| `select(Class, sql, Object...)` / `select(sql, Map, JdbcTemplateCallback)` / `select(sql, Object...)` 三个重载 | SQL 自动追加 `is_deleted` 条件与 `AND group_id = ?`/`:group_id`，参数自动补齐 |
| `getUserId()` / `getGroupId()` | 从 `UserContextHolder` 取值；**无上下文或字段为 null 时兜底 `1L`** |

**关键语义**：
- 你写的 SQL **不需要也不应该**包含 `group_id` 条件——DAO 会追加。手写会导致重复条件（虽然通常不报错，但参数顺序易错）
- select 路径同时追加逻辑删除过滤（`is_deleted`），查询结果天然排除已删除数据
- **兜底 `1L` 是静默的**：异步线程/定时任务/未认证请求中操作数据库，会读写 group 1 的数据。多租户敏感操作前必须确认 `UserContextHolder.get() != null`

**⚠️ 覆盖范围**：上表列出的是本模块**显式重写**的方法。`TableDAO` 其余继承方法（如 sharp-database 提供的其他查询变体）是否带 `group_id` 过滤取决于基类实现，本模块未重写。[需要确认：使用未重写方法前，先在 sharp-database 源码核实]

**不要**：
- 不要 `new ExtendTableDAOImpl(...)` 自建 DAO——会丢失全部租户逻辑
- 不要绕过 `TableDAO`/`BaseServiceImpl` 直接用 `JdbcTemplate` 写业务 SQL（无租户隔离）

### 2.2 保存回填：`InsertUpdateCallback` + `GroupIdGetter` ⭐（自动）

`DatabaseConfig#insertCallback` 注册回调：每次 insert/update 后，若实体实现 `GroupIdGetter`（三个 `Component*Entity` 基类都已实现），自动把参数中的 `group_id` 写回实体的 `groupId` 字段。

**效果**：`baseService.insertOrUpdate(entity)` 之后，`entity.getGroupId()` 已有值，无需再查库。

```java
public interface GroupIdGetter {
    void setGroupId(Long id);
}
```

`GroupIdGetter` 本身 ⚠️：只在自定义实体基类（不继承 `Component*Entity` 但又想被回填）时才需要手动实现——正常情况下继承 §3 的基类即可，不要单独使用。

---

## 3. 基础实体类

site/erp 的所有数据库实体**必须**继承下列之一（而不是 sharp-database 的 `BaseEntity` / `BaseCodeEntity` / `BaseCodeDescriptionEntity`）：

| 基类 ⭐ | 继承自（sharp-database） | 适用实体 |
| --- | --- | --- |
| `ComponentBasEntity<ID>` | `BaseEntity<ID>` | 普通实体 |
| `ComponentBaseCodeEntity<ID>` | `BaseCodeEntity<ID>` | 带 `code` 的实体（配合 `BaseCodeApi`） |
| `ComponentBaseCodeDescriptionEntity<ID>` | `BaseCodeDescriptionEntity<ID>` | 带 `code` + `description` 的实体 |

三者统一新增字段：

```java
@Column(value = "group_id", updatable = false, comment = "所属公司id")
@JsonSerialize(using = ToStringSerializer.class)
private Long groupId;
```

**语义与约束**：
- `updatable = false`：update 语句不会修改 `group_id`（租户归属不可变更）
- JSON 序列化为**字符串**（避免前端 JS Long 精度丢失）——接口消费方拿到的是 `"groupId": "1"` 不是数字
- 保存后由 §2.2 回调自动回填，业务代码**不要手动 `setGroupId()`**
- 均支持 Lombok `@SuperBuilder`，子类需同样标注 `@SuperBuilder` + `@NoArgsConstructor` + `@AllArgsConstructor`

**真实示例**（erp `Plant`）：

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
@Table(value = "mm_plant", comment = "库房")
public class Plant extends ComponentBaseCodeDescriptionEntity<Long> {
    @NotBlank
    private String code;

    @Column(columnDefinition = "json")
    private List<Long> areaPath;      // PG json 列，list 查询返回时自动转 JsonNode（见 §4 flattenKeys）

    @Length(max = 128, message = "详细地址不能超过128个字符")
    private String detailAddress;
}
```

---

## 4. 通用 REST 控制器基类

### 4.1 `BaseApi<S, T, ID>` ⭐

**用途**：继承即获得整套标准 CRUD REST 端点。泛型：`S` = `BaseServiceImpl` 子类（sharp-database），`T` = 实体（`EntityId<ID>` 子类），`ID` = 主键类型。

**接入方式**（唯一正确姿势）：

```java
@RestController
@RequestMapping("plants")            // 实际路径自动变为 /{groupId}/plants
public class PlantController extends BaseApi<PlantService, Plant, Long> {
    public PlantController(PlantService baseService) {
        super(baseService);          // 必须：构造器传入 service
    }
}
```

**端点一览**（路径相对于子类 `@RequestMapping`，且全部带 `/{groupId}` 前缀）：

| 端点 | 方法 | 返回 | 语义 |
| --- | --- | --- | --- |
| `GET ""` | `list` | `Grid<Map<String,Object>>` | 分页列表。查询条件直接来自 query 参数（由实体 TableMeta 的 selectConditionSQL 生成 where）。行数据经 `flattenKeys` 处理：`"a.b"` 键改为 `"aB"`、**null 值键被移除**、PG `PGobject`（json 列）转为 `JsonNode` |
| `GET "detail"` | `listCascade` | `Grid<T>` | 先执行 `list`，再按结果 id 集合 `selectByIds` 返回**强类型实体**分页。rows 为空时返回空 Grid |
| `GET "one"` | `one` | `T` | 按 query 参数条件查单条；查不到抛 `ResourceNotFoundException` |
| `GET "new"` | `newEntity` | `T` | 返回空白实体实例（前端表单初始化用） |
| `GET "{id}"` | `findById` | `T` | 按主键查；不存在抛 `ResourceNotFoundException` |
| `POST ""` | `saveOrUpdate` | `T` | `@Valid` 校验 body，`baseService.insertOrUpdate`（有 id 更新、无 id 插入），返回带 id 的实体 |
| `PUT "{id}"` | `update` | `T` | `@Valid` 全量更新，path id 覆盖 body id |
| `PATCH "{id}"` | `patch` | `T` | 部分更新（委托 `baseService.patch`） |
| `DELETE "{id}"` | `deleteById` | `Result<?>` | 委托 `baseService.deleteById`，结果包 `ResultUtils.success`。select 路径自动过滤 `is_deleted`（表结构支持逻辑删除；deleteById 具体是逻辑删还是物理删由 sharp-database 决定 [需要确认]） |

**protected 扩展点**（子类可覆写）：

```java
protected T getEntityFromOptional(Optional<T> optional, Object key) // 空值 → 抛异常
protected ResourceNotFoundException getResourceNotFoundException(Object key) // 定制 404 消息
protected String comment()   // 表 comment，用于异常消息
```

**public 工具**：

```java
public static Map<String, Object> flattenKeys(Map<String, Object> source)
```
⚠️ 特定场景使用：自定义 list 类端点想复用同样的键扁平化/PGobject 转换逻辑时调用；其他场景不要用。

**注意事项**：
- `list` 的分页参数、条件拼装由 sharp-database 的 `GridUtils`/`SQLParamCleaner` 完成，**不要自行计算 total、自行封装分页对象**
- `@Valid` 只在 POST/PUT/PATCH 生效，校验失败由统一异常处理转 JSON
- 租户隔离自动生效（§2），子类端点无需关心 `group_id`

### 4.2 `BaseCodeApi<S, T, ID>` ⭐

**用途**：带 `code` 业务编码的实体（`T extends BaseCodeEntity<ID>`，含 `ComponentBaseCode*Entity` 子类）在 `BaseApi` 之上增加按 code 查询。

| 端点 | 方法 | 返回 | 语义 |
| --- | --- | --- | --- |
| `GET "codes/{code}"` | `findByCode` | `T` | 按 code 查单条；不存在抛 `ResourceNotFoundException`，错误码 `CODE_NOT_EXISTS_ERROR(400, "%s code「%s」不存在")` |

```java
@RestController
@RequestMapping("plants")
public class PlantController extends BaseCodeApi<PlantService, Plant, Long> {
    public PlantController(PlantService baseService) { super(baseService); }
}
```

Service 侧配套：`PlantService.selectByCode("C0013")` 返回 `Optional<T>`（sharp-database `EntityCodeDAO` 能力）。

### 4.3 `BaseFormController<S, T, ID>` ⚠️

**用途**：服务端渲染表单页（Thymeleaf 风格，返回视图名）+ JSON 增删改混合控制器。依赖 sharp-meta 字典（`DictUtils`）。只在需要服务端表单页时使用；纯前后端分离 API 用 `BaseApi`。

**构造器**：`BaseFormController(S service, String formPage)` —— `formPage` 是视图名。

| 端点 | 返回 | 语义 |
| --- | --- | --- |
| `GET "new"` | 视图 `formPage` | Model 注入：空实体（属性名 = 实体类名首字母小写；`List`/`Set` 字段初始化为空集合）、`query`（请求参数 map）、所有字典（枚举/`@DictType`/`DictValue` 字段 → `DictUtils.getDict`） |
| `GET "{id}/page"` | 视图 `formPage` | Model 注入：查库实体（不存在抛 `ResourceNotFoundException`）、字典 label 回填、`query`、`readonly` 标记（`HtmlTagUtils`）；同样注入字典 |
| `POST ""` | `Result`（JSON） | `@Valid` 保存，返回 `ResultUtils.success(id)` |
| `PUT "{id}"` | `Result`（JSON） | 更新，返回 id |
| `DELETE "{id}"` | `Result`（JSON） | 删除 |

**约束**：实体泛型位置有要求——构造器通过 `ClassUtils.getClassGenericsTypes(this.getClass())[1]` 取实体类，即**子类声明泛型时实体必须在第 2 个位置**（继承本类时天然满足）。

### 4.4 `SqlReportApi` 🚫（demo 性质，不要依赖/扩展）

`@RestController @RequestMapping("api")`，端点 `GET "api/sql/{key}"`（实际 `/{groupId}/api/sql/{key}`）：按 key 从**硬编码静态 Map** 取预注册 SQL 执行分页查询；key 不存在抛 `ResourceNotFoundException(key)`。

当前仅注册 `mm_material`、`masters` 两个 key，且 SQL 要求"必须把 id 查询出来"。新增 key 需要改本模块源码——**不要把它当可配置报表引擎使用**，业务报表请在各自服务内实现。

---

## 5. 异常与错误码

### 5.1 统一异常处理（自动）⭐

`ComponentConfig` 上 `@ComponentScan(basePackageClasses = ApiExceptionHandler.class)` 自动注册 sharp-common 的 `ApiExceptionHandler`。**业务代码抛异常即可，不要自己 try-catch 转 JSON 响应**。

### 5.2 `ResourceNotFoundException` ⭐

继承 sharp-common `BizException`。资源不存在时使用：

```java
public ResourceNotFoundException()                                  // 消息：「资源」不存在
public ResourceNotFoundException(String objectName)                 // 消息：「{objectName}」不存在
public ResourceNotFoundException(ExceptionCode code, Object[] params) // 自定义错误码 + 消息参数
```

```java
Plant plant = plantService.selectById(id)
        .orElseThrow(() -> new ResourceNotFoundException("库房"));
```

**不要**：不要用裸 `RuntimeException` / 自造异常代替——会绕过统一错误码格式。

### 5.3 `ExceptionCodeEnum` ⭐

实现 sharp-common `ExceptionCode`，可直接用于 `BizException`：

| 枚举 | code | message 模板 |
| --- | --- | --- |
| `PARAM_ERROR` | 400 | 参数传递错误 |
| `CODE_NOT_EXISTS_ERROR` | 400 | `%s code「%s」不存在` |
| `DUPLICATE_CODS_ERROR` | 400 | `%s code「%s」不能重复`（注意枚举名拼写即 CODS） |
| `DUPLICATE_DATA_ERROR` | 400 | `「%s」出现重复数据` |
| `CODE_EXISTS_ERROR` | 400 | `%s code「%s」已经存在` |
| `CODES_NOT_EXISTS_ERROR` | 400 | `%s code「%s」不存在` |
| `REQUIRED_ERROR` | 400 | `「%s」必填` |
| `CHECK_NOT_VALID_ERROR` | 500 | `%s` |
| `RESOURCE_NOT_EXISTS_ERROR` | 4004 | `「%s」不存在` |

```java
throw new BizException(ExceptionCodeEnum.REQUIRED_ERROR, new Object[]{"code"});
```

业务服务可仿照此枚举定义自己的错误码枚举（如 erp 的 `com.rick.erp.module.common.exception.ExceptionCodeEnum`），实现同一 `ExceptionCode` 接口即可被统一处理。

---

## 6. Web 装配（三个 @Import 配置类）

**接入方式唯一**：在消费服务启动类 `@Import`，不要继承、不要实例化其中内容。

```java
@SpringBootApplication
@DependsOn("entityDAOSupport")   // sharp-database 要求，见消费方现有启动类
@Import({ComponentConfig.class, WebMvcRegistrationsConfig.class, DatabaseConfig.class})
@EnableDubbo                      // 需要 Dubbo 时
public class ErpApplication { ... }
```

### 6.1 `ComponentConfig` 🚫（仅 @Import；头常量 ⭐ 可引用）

继承 sharp-common `SharpWebMvcConfigurer`，装配三件事：

1. **拦截器**：读 `X-User-Id`/`X-User-Mobile` 头 + 路由 `{groupId}` 路径变量 → `UserContextHolder.put`；`afterCompletion` 清理。**头缺失不阻断请求**（匿名可进，上下文为 null）
2. **参数解析器**：Controller 方法的 `User` 参数 ← `UserContextHolder.get()`（§1.1）
3. **转换器**：`IdToEntityConverterFactory` —— GET 请求允许 `"1"` 直接映射为实体参数的 id（`Person person = "1"` → `person.setId(1L)`）。⚠️ 不常用，知道即可

### 6.2 `WebMvcRegistrationsConfig` 🚫

定制 `RequestMappingHandlerMapping`：**给所有 Controller 的映射路径前置 `{groupId}`**（`BasicErrorController` 除外）。

后果（必须知道）：
- `@RequestMapping("plants")` + `@GetMapping("{id}")` 的完整路径是 `/{groupId}/plants/{id}`
- 网关路由 `StripPrefix=2` 剥掉 `/api/site` 后，`/{groupId}` 段保留，由拦截器解析为租户号
- 本地直调服务测试时必须带前缀：`GET http://localhost:7002/1/plants`
- `{groupId}` 是路径变量而非正则约束——任何值都匹配，非数字会导致拦截器 `Long.parseLong` 抛 `NumberFormatException`

### 6.3 `DatabaseConfig` 🚫

提供两个 Bean，详见 §2：
- `tableDAO`（`@Primary TableDAO`）：group_id 自动拼接 + 兜底 1
- `insertCallback`（`InsertUpdateCallback`）：保存后回填实体 `groupId`

**可否覆盖**：`tableDAO` 是 `@Primary`，消费方如自定义同类型 Bean 会产生冲突/优先级问题——**不要覆盖**，有多租户定制需求应修改本模块（需人工评审）。

### 6.4 本模块自身无配置项

无 `@ConfigurationProperties`、无 yml 键。行为由请求头、路由前缀和 `@Import` 决定。数据库/Nacos/Dubbo 连接配置属于各消费服务自己的 yml（占位符经 `.env.*.properties` 注入，见根 CLAUDE.md）。

---

## 7. Dubbo 身份透传（自动生效）

### 7.1 两个 Filter 🚫（内部实现，业务代码零调用）

| Filter | 激活侧 | 行为 |
| --- | --- | --- |
| `UserContextConsumerFilter` | `@Activate(group = CONSUMER)` | 发起调用时把 `UserContextHolder` 中的 id/mobile/groupId 写入 attachment（键 = `X-User-Id`/`X-User-Mobile`/`X-Group-Id`）；上下文为 null 则不写 |
| `UserContextProviderFilter` | `@Activate(group = PROVIDER)` | 从 attachment 还原 `User` 放入 `UserContextHolder`；attachment 无 userId 则跳过；调用结束**恢复原上下文**（injvm 嵌套调用不破坏外层） |

**注册方式**：SPI 文件 `META-INF/dubbo/org.apache.dubbo.rpc.Filter`（`userContextConsumer` / `userContextProvider`）。`@Activate` + group 意味着**只要 jar 在 classpath 上，消费端/提供端自动激活，无需任何 yml 配置**。

**效果**：erp 的 Controller（HTTP 上下文）→ `@DubboReference DemoService` → platform/site 的 `@DubboService` 内 `UserContextHolder.get()` 拿到同一用户，`TableDAO` 的 `group_id` 隔离跨服务一致。链式 Dubbo 调用（A→B→C）天然支持。

**注意**：
- 消费端在**无上下文线程**（@Async/定时任务）发起 Dubbo 调用 → attachment 为空 → 提供端上下文为 null → 数据库兜底 group 1
- 不要手动 `invocation.setAttachment("X-User-Id", ...)` 伪造身份
- Dubbo 接口本身定义在 `common/component-api` 模块，不在本模块

---

## 8. 传递依赖

本模块以 `api` 方式暴露下列依赖——消费方 `implementation project(':component-starter')` 后**无需重复声明**：

| 依赖 | 提供能力 |
| --- | --- |
| `spring-boot-starter-web` / `-jdbc` / `-validation` | Servlet Web、JdbcTemplate、`@Valid` |
| `com.rick.db:sharp-database` | `BaseServiceImpl`/`TableDAO`/`EntityDAO`/`Grid` 分页/实体注解（`@Table`/`@Column`） |
| `com.rick.meta:sharp-meta` | 字典（`DictUtils`/`@DictType`/`DictValue`），`BaseFormController` 依赖 |
| `com.rick.formflow:sharp-formflow` | 表单流 |
| `com.rick.fileupload:sharp-fileupload` | 文件上传（`DocumentController` 等，消费方自行 `@Import`） |
| `com.rick.excel:sharp-excel` | Excel（排除了 sharp-database2 / sharp-common 传递项） |
| `org.postgresql:postgresql` | PG 驱动 |
| `springdoc-openapi-starter-webmvc-ui 2.8.14` | Swagger UI |
| `dubbo-spring-boot-starter` / `dubbo-nacos-spring-boot-starter` | Dubbo |
| `spring-cloud-starter-alibaba-nacos-discovery` | Web 实例注册（`WEB_GROUP`，与 Dubbo 实例隔离） |

---

## 9. Common Mistakes

### 错误 1：忘记 @Import 三个配置类

```java
// ❌ 只有 @SpringBootApplication
// 症状：接口能通但 URL 无 {groupId} 前缀、User 参数恒为 null、SQL 不带租户条件
@SpringBootApplication
public class SiteApplication { ... }

// ✅
@SpringBootApplication
@Import({ComponentConfig.class, WebMvcRegistrationsConfig.class, DatabaseConfig.class})
public class SiteApplication { ... }
```

**原因**：本模块不是自动装配 starter（无 `AutoConfiguration.imports`），三个配置类必须显式导入，且各管一件事，缺一少一。

### 错误 2：手写 group_id 条件

```java
// ❌ 重复条件 + 参数易错位；且兜底逻辑失效
tableDAO.select("SELECT * FROM mm_plant WHERE code = ? AND group_id = ?", code, groupId);

// ✅ 直接注入 TableDAO 写业务条件即可
tableDAO.select(Plant.class, "SELECT * FROM mm_plant WHERE code = ?", code);
```

**原因**：注入的 `TableDAO` 已在 select/update 路径自动追加 `AND group_id = ?` 与 `is_deleted` 条件（§2.1）。

### 错误 3：绕过 UserContextHolder 解析请求头

```java
// ❌
Long userId = Long.parseLong(request.getHeader("X-User-Id"));

// ✅ Controller
public String foo(User user) { ... }
// ✅ 非 Controller 层
User user = UserContextHolder.get();   // 判空
```

**原因**：头的解析、`{groupId}` 合并、生命周期清理已由拦截器完成；手动解析在头缺失时直接 NPE，且拿不到 groupId。

### 错误 4：异步线程丢上下文

```java
// ❌ @Async 方法内 UserContextHolder.get() == null，数据库静默落到 group_id=1
@Async
public void export() { tableDAO.select(...); }

// ✅ 提交异步任务前把需要的值取出作为参数传递
User user = UserContextHolder.get();
executor.submit(() -> doExport(user.getId(), user.getGroupId()));
```

**原因**：上下文是 ThreadLocal，不跨线程传播；兜底 `1L` 是静默的，出错表现为"数据串到 group 1"而不是异常。

### 错误 5：调用自身/兄弟服务 URL 忘记 {groupId} 前缀

```java
// ❌ 404：GET http://localhost:7002/plants
// ✅ GET http://localhost:7002/1/plants   （经网关则是 /api/erp/1/plants）
```

**原因**：`WebMvcRegistrationsConfig` 给所有映射加了 `{groupId}` 前缀（§6.2）。

### 错误 6：@DubboReference 标在 final 字段

```java
// ❌ 类上有 @FieldDefaults(level = PRIVATE) 时 final 字段无法被 Dubbo 注入
@DubboReference
private final DemoService demoService;

// ✅
@DubboReference
private DemoService demoService;
```

**原因**：erp 现有代码（`PlantController`）中已用注释标出此坑。

### 错误 7：实体直接继承 sharp-database 基类

```java
// ❌ 丢失 groupId 字段：JSON 输出无 groupId、保存后无法回填、@Column 定义缺失
public class Plant extends BaseCodeDescriptionEntity<Long> { ... }

// ✅
public class Plant extends ComponentBaseCodeDescriptionEntity<Long> { ... }
```

### 错误 8：把 SqlReportApi 当配置化报表

**症状**：调用 `GET /{groupId}/api/sql/{myKey}` 返回「{myKey}」不存在。
**原因**：SQL 映射是模块内硬编码静态 Map（仅 `mm_material`/`masters`），新增需改模块源码。业务报表在自己服务内实现。

### 错误 9：在 gateway（WebFlux）引入本模块

**原因**：拦截器/参数解析器/ThreadLocal 全部基于 Servlet API；gateway 的身份机制见根 CLAUDE.md（`ReactiveSecurityContextHolder`）。

### 错误 10：修改本模块以修复 platform 的同类问题

**原因**：platform 不依赖本模块，维护自己的 `UserContextHolder`/`DatabaseConfig`/拦截器副本。两边需要分别修改，不要假设联动。
