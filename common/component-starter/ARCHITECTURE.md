# component-starter 架构说明

> 面向 AI 编程助手：解释模块的设计与请求生命周期，帮助判断"为什么这样用"。
> API 细节 → `API.md`；使用规则 → `CLAUDE.md`。

## 1. 模块定位与边界

```
gateway (WebFlux, 无状态 token 认证)
   │  HTTP 转发前覆盖写注入 X-User-Id / X-User-Mobile（UserHeaderGlobalFilter）
   │  路由 /api/site/**、/api/erp/** → StripPrefix=2 → /{groupId}/...
   ▼
component-site / component-erp (Servlet 栈)          ◀── 本模块的唯一消费方
   │  本模块负责：解析身份头 + {groupId} → UserContextHolder
   │             TableDAO 自动 group_id 租户隔离
   │             BaseApi 系列通用 CRUD
   │  Dubbo 调用时本模块 Filter 自动透传身份 attachment
   ▼
其他后端服务（@DubboService 提供方）

platform：不依赖本模块，维护自己的等价实现副本
（com.rick.platform.config.DatabaseConfig / UserContextWebConfig / module.user.UserContextHolder）
```

**打包形态**：纯库 jar（`bootJar` 禁用）。group `com.rick.common.component.starter`，随主工程版本（`libs.versions.sharp`）。

**为什么不是自动装配**：模块无 `META-INF/spring/...AutoConfiguration.imports`，消费方必须在启动类显式
`@Import({ComponentConfig.class, WebMvcRegistrationsConfig.class, DatabaseConfig.class})`。
这是有意保留的接入仪式感：三个配置类各自独立生效，消费方可以看清自己启用了哪些能力（代价是漏 Import 时静默降级，见 API.md §9 错误 1）。

## 2. 三个配置类的职责划分

| 配置类 | 职责 | 生效机制 |
| --- | --- | --- |
| `ComponentConfig` | 身份**入口**：拦截器写 `UserContextHolder`；`User` 参数解析器；id→实体转换器；扫描注册统一异常处理 `ApiExceptionHandler` | `@Configuration` + 继承 sharp-common `SharpWebMvcConfigurer` |
| `WebMvcRegistrationsConfig` | 租户**路由约定**：所有 Controller 映射自动前置 `{groupId}` 路径变量（`BasicErrorController` 除外） | Spring Boot `WebMvcRegistrations` 扩展点，替换 `RequestMappingHandlerMapping` |
| `DatabaseConfig` | 租户**数据出口**：`@Primary TableDAO` 在 insert/update/select 路径自动拼 `group_id`；`InsertUpdateCallback` 保存后回填实体 `groupId` | 两个 `@Bean` |

三者共同构成"多租户身份链"在本服务内的闭环：**路由带租户号 → 拦截器建上下文 → DAO 用上下文隔离数据**。

## 3. HTTP 请求生命周期

```
GET /api/erp/1/plants?code=C001        （客户端 → 网关）
  │ 网关：认证 token → 覆盖写 X-User-Id: 42, X-User-Mobile: 138...
  │ 网关：StripPrefix=2 → 转发 /1/plants?code=C001 到 erp (7002)
  ▼ erp 内（本模块接管）
  1. RequestMappingHandlerMapping（已定制）
       /1/plants 匹配 "{groupId}" + "plants" 的组合映射
  2. ComponentConfig 拦截器 preHandle
       读 X-User-Id → User{id=42, mobile=138...}
       读 URI 模板变量 groupId=1 → user.groupId=1
       UserContextHolder.put(user)          ← ThreadLocal
       （头缺失时跳过，不阻断 → 上下文为 null）
  3. Controller 执行
       - 方法声明 User 参数 → 参数解析器直接回 UserContextHolder.get()
       - BaseApi.list() → GridUtils 分页 → 注入的 TableDAO
           select 路径自动改写：... WHERE code = :code AND is_deleted... AND group_id = :group_id
           （group_id 取上下文，无上下文兜底 1）
       - insert/update → addInsertInfo 自动放 group_id
           → InsertUpdateCallback 把 group_id 回填实体（GroupIdGetter）
       - 抛 BizException/ResourceNotFoundException → ApiExceptionHandler 转 JSON 错误码
  4. 拦截器 afterCompletion
       UserContextHolder.remove()           ← 防线程池复用泄漏
```

## 4. Dubbo 调用生命周期

```
erp Controller（线程内已有 UserContextHolder）
  │ @DubboReference DemoService.sayHello(...)
  ▼
UserContextConsumerFilter（@Activate CONSUMER，SPI 自动激活）
  │ attachment: X-User-Id=42, X-User-Mobile=138..., X-Group-Id=1
  ▼
提供方服务（如 site 的 @DubboService）
UserContextProviderFilter（@Activate PROVIDER）
  │ 还原 User → UserContextHolder.put
  │ try { 业务执行（TableDAO 同样按 group_id 隔离） }
  │ finally { 恢复调用前上下文（远程调用原值 null → remove；injvm 嵌套不破坏外层） }
  ▼
链式调用 A→B→C 天然支持：每一跳消费端读的都是自己线程的 ThreadLocal
```

注册：`META-INF/dubbo/org.apache.dubbo.rpc.Filter`（`userContextConsumer` / `userContextProvider`），classpath 上有本模块即自动生效，无 yml 开关。

**跨线程边界即失效**：HTTP→Dubbo 靠 Filter 接力，但 ThreadLocal 不会自动进入 `@Async`/线程池/定时任务——这些场景中上下文为 null，数据库静默兜底 `group_id=1`（API.md §9 错误 4）。

## 5. 多租户（group_id）机制要点

- **租户号来源**：路由路径变量 `{groupId}`（经网关的请求必带），不是请求头
- **写入**：insert 由 `addInsertInfo` 自动放入参数 map
- **过滤**：`update` 的 2 个重载、`select` 的 3 个重载在本模块被显式重写追加条件；`TableDAO` 其余继承方法是否过滤取决于 sharp-database 基类，本模块未重写 [需要确认]
- **不可变更**：实体 `groupId` 列 `updatable = false`
- **兜底策略**：无上下文或字段为 null → `1L`。这是为开发期/系统任务留的静默兜底，多租户敏感路径必须自行确认上下文存在
- **序列化契约**：实体 `groupId` 经 `ToStringSerializer` 输出为字符串（前端 Long 精度）

## 6. 通用控制器的设计取舍

- `BaseApi` 是**泛型继承式**复用（非注解式）：子类只提供 `@RequestMapping` 路径与构造器，端点集合固定；定制靠覆写 protected 扩展点（`getResourceNotFoundException`/`comment`），或并列新增自己的 `@GetMapping` 方法（如 erp `PlantController.getSites`）
- `list` 返回 `Grid<Map>`（弱类型、键扁平化、PGobject→JsonNode），`detail` 返回 `Grid<T>`（强类型、多一次 `selectByIds`）——前端表格用 `list`，需要实体结构时用 `detail`
- `BaseFormController` 是服务端渲染时代的形态（返回视图名 + Model 注入字典），与 `BaseApi` 并存但定位不同；新前后端分离 API 一律用 `BaseApi`/`BaseCodeApi`
- `SqlReportApi` 是硬编码 SQL 映射的 demo，不是扩展点

## 7. 与外部快照库的关系

本模块是 sharp 系列库在业务服务中的**装配层**，自身几乎无业务逻辑：

| 外部库（mavenLocal） | 本模块如何用 |
| --- | --- |
| `com.rick.db:sharp-database` | 继承 `ExtendTableDAOImpl` 加租户逻辑；`BaseApi` 依赖 `BaseServiceImpl`/`EntityDAO`/`Grid`/`GridUtils`/`SQLParamCleaner`；实体基类继承其 `BaseEntity` 系列 |
| `com.rick.common`（sharp-common，经传递依赖） | `ApiExceptionHandler` 统一异常、`BizException`/`ExceptionCode`、`Result`/`ResultUtils`、`SharpWebMvcConfigurer`、`HttpServletRequestUtils` |
| `com.rick.meta:sharp-meta` | `BaseFormController` 的字典注入（`DictUtils`/`@DictType`/`DictValue`） |

排查涉及这些库内部行为的问题（如 `GridUtils` 分页参数名、`SQLParamCleaner` 的条件生成规则）时，需要读对应快照库源码，本模块文档不覆盖。[需要确认：快照库源码位置在 ~/.m2 对应构件或独立仓库]

## 8. 演进注意事项

- 修改 `DatabaseConfig` 的兜底值（`1L`）或拼接逻辑会影响 site/erp **所有** SQL 路径，属高危变更
- 修改 `WebMvcRegistrationsConfig` 的前缀约定会同时要求网关路由、前端调用、集成测试全部联动
- 新增能力优先放进三个既有配置类的职责边界内；新增第四个配置类需同步更新消费方 `@Import` 与本模块三份文档
- platform 的副本实现与本模块**刻意不共享代码**；统一二者是架构级决策，不要在普通需求中顺手做
