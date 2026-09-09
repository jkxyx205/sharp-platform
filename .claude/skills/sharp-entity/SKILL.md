---
name: sharp-entity
description: 在 sharp-platform 业务模块（component-site / component-erp 等）中创建实体并完成最基本的 CRUD（含 REST API 访问）。当用户说「在模块 X 下，包 Y 创建实体 Z，字段包含 …」或类似请求时使用。生成 Entity + 枚举 + DAO + Service + Controller 五件套，并编译验证、给出建表与端点清单。
---

# sharp-entity：实体 + 基础 CRUD 生成

按用户给定的 **模块、包名、实体名、字段清单**，在 sharp-platform 业务模块中生成完整 CRUD 链路。
所有约定以 `common/component-starter/API.md` 为准，本 skill 是其操作化配方。
参考实现（已验证可编译 + 建表）：`component-site` 的 `com.rick.site.module.site`（Site / SiteStatus / SiteDAO / SiteService / SiteController）；分类字段（RowCategory）场景见 `com.rick.site.module.page`（Page / PageDAO / PageService / PageController）；枚举分类字段场景见 `com.rick.site.module.user`（User / UserType / UserDAO）。

## 输入解析

从用户提示词提取：

| 输入 | 示例 | 缺省时 |
| --- | --- | --- |
| 模块 | component-site | 必须询问，不要猜 |
| 包名 | com.rick.site.module.site | 按模块惯例 `com.rick.{app}.module.{模块名}` |
| 实体名 | Site | 必须有 |
| 字段清单 | code、name、status(NEW 新建/ACTIVE 激活) | 必须有；id 不用列出（基类自带） |
| 分类字段（可选） | 「siteId 作为分类字段」 | 默认无分类；用户明确指定时走第 1 步的 RowCategory 分支 |

## 第 1 步：选择基类链（关键决策）

按字段清单是否含 `code` / `description` 选择，四层必须配套：

| 实体字段 | 实体基类（starter） | DAO | Service | Controller |
| --- | --- | --- | --- | --- |
| 有 code + description | `ComponentBaseCodeDescriptionEntity<Long>` | `EntityCodeDAOImpl` | `BaseCodeServiceImpl` | `BaseCodeApi` |
| 有 code | `ComponentBaseCodeEntity<Long>` | `EntityCodeDAOImpl` | `BaseCodeServiceImpl` | `BaseCodeApi` |
| 都没有 | `ComponentBaseEntity<Long>` | `EntityDAOImpl` | `BaseServiceImpl` | `BaseApi` |

基类已自带：`id`、审计字段（create_by/create_time/update_by/update_time/is_deleted）、`groupId`（多租户，自动填充回填）。
**用户字段清单里的 id 不要重复声明**；`code` 需要挂 `@NotBlank` 时在子类重新声明（既有 demo `Plant` 的写法）。

### 分类字段分支（RowCategory）

用户指定某字段为**分类字段**（如「siteId 作为分类字段」）时，在上表基础上叠加两处变化，Service/Controller 层不变：

1. **实体**额外实现 `RowCategory<E>`（E = 分类字段类型），`getCategory()/setCategory()` 委托给实际字段
2. **DAO** 换成 Category 版本（泛型多一个 E，即分类值类型）：

| 实体 | DAO（分类版） |
| --- | --- |
| 无 code | `CategoryEntityDAOImpl<T, Long, E>` |
| 有 code | `CategoryEntityCodeDAOImpl<T, Long, E>` |
| 分类字段是枚举 | 无 code → `CategoryEnumEntityDAOImpl<T, Long, E>`；有 code → `CategoryEnumEntityCodeDAOImpl<T, Long, E>`（E extends Enum，**DB 按枚举 code 存取**，与建表 CHECK IN (codes)、实体写入、valueOfCode 读取全链一致；枚举模板 getCode() 返回 name()，天然满足 code = name） |

分类 DAO 在标准 CRUD 之外额外提供（Service 层经 `baseService.getBaseDAO()` 或注入 DAO 使用）：

- `selectAll(分类值)` — 查某分类下全部实体
- `insertOrUpdate(分类值, 实体集合)` — 整组替换（对齐增删改）
- `selectByCategoryAndCode(分类值, code)` — 仅 code 版

分类列名规则对 Enum 版同样适用：无参构造默认 `"category"`，实体分类字段不叫 category 时（如 `type`），DAO 构造器**必须**显式 `super("type")`。

## 第 2 步：生成文件（包结构固定）

```
{包名}/
├── entity/Xxx.java          # 实体
├── entity/XxxStatus.java    # 枚举字段（每个枚举一个文件，放 entity 包）
├── dao/XxxDAO.java
├── service/XxxService.java
└── controler/XxxController.java   # ⚠️ 包名沿用目标模块既有拼写（site/erp 现为 controler）
```

**硬约束**：实体必须落在 `**.entity` 结尾的包下（`sharp.database.entity-base-package: com.rick.{app}.**.entity` 扫描约定），否则 TableMeta 不识别。DAO/Service/Controller 靠 `@Repository`/`@Service`/`@RestController` 被启动类包扫描，无需任何注册。

### 模板（以 Site 为例，替换名称/字段即可）

**实体**（Lombok 六件套注解缺一不可，`@SuperBuilder` 要求基类链一致）：

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "site_site", comment = "站点")   // 表名 = 模块前缀_snake_case，沿用模块内既有前缀惯例
public class Site extends ComponentBaseCodeEntity<Long> {

    @NotBlank
    String code;

    @NotBlank
    @Column(comment = "站点名称")
    String name;

    @Column(comment = "状态")
    SiteStatus status;
}
```

**枚举字段**（框架强约定，缺 `getCode()` 建表直接 RuntimeException）：

```java
@AllArgsConstructor
@Getter
public enum SiteStatus {
    NEW("新建"),
    ACTIVE("激活");

    @JsonValue
    public String getCode() {
        return this.name();
    }

    private final String label;

    public static SiteStatus valueOfCode(String code) {
        return valueOf(code);
    }
}

```

- String code → 建表 `VARCHAR(32)` + `CHECK IN (...)`；Number code → `INTEGER`
- JSON 请求体按 code 传值（`"status": "NEW"`）；响应输出枚举 name
- 无 code 时 `valueOfCode` 可省（框架回退 `Enum.valueOf`），但建议保留

**DAO / Service / Controller**：

```java
@Repository
public class SiteDAO extends EntityCodeDAOImpl<Site, Long> { }

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SiteService extends BaseCodeServiceImpl<SiteDAO, Site, Long> {
    public SiteService(SiteDAO siteDAO) { super(siteDAO); }
}

@RestController
@RequestMapping("sites")   // 实体名复数 snake_case
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SiteController extends BaseCodeApi<SiteService, Site, Long> {
    public SiteController(SiteService baseService) { super(baseService); }
}
```

Controller 类注释中列出继承得到的端点清单（见第 4 步），不写任何自定义方法——"最基本的 CRUD" 零代码即得。

**分类字段变体**（以 Page 为例，已验证建表 + 编译；只列与标准模板的差异，其余字段/Service/Controller 完全相同）：

```java
@Table(value = "site_page", comment = "页面")
public class Page extends ComponentBaseEntity<Long> implements RowCategory<Long> {

    @Column(comment = "所属站点id")
    Long siteId;

    @Override
    @JsonIgnore   // 避免 JSON 输出与 siteId 重复
    public Long getCategory() {
        return siteId;
    }

    @Override
    public void setCategory(Long category) {
        this.siteId = category;
    }
}

@Repository
public class PageDAO extends CategoryEntityDAOImpl<Page, Long, Long> {

    public PageDAO() {
        super("site_id");   // ⚠️ 必须显式传分类列名：无参构造默认 "category"，列不存在会 SQL 报错
    }
}
```

**分类字段是枚举**（以 User 为例：字段 `UserType type` → 列 `type`，DB 按枚举 name 存取）：

```java
@Repository
public class UserDAO extends CategoryEnumEntityDAOImpl<User, Long, UserType> {

    public UserDAO() {
        super("type");   // ⚠️ 传的是分类字段的 DB 列名（字段名 snake_case），不是类型名 "UserType"，也不是默认 "category"
    }
}
```

## 第 3 步：编译验证（必须执行）

```bash
export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"
gradle -p /Users/rick/Space/Workspace/sharp-platform :{模块名}:compileJava
```

`@SuperBuilder` 泛型的 unchecked 警告属正常（与既有实体一致），不算失败。

## 第 4 步：建表 + 汇报

**建表**：在模块 `src/test/java/.../TableGeneratorTest.java` 中追加/修改 `tableGenerator.createTable(Xxx.class)`，提示用户执行（需本地 PostgreSQL 运行）：

```bash
gradle -p ... :{模块名}:test --tests "com.rick.{app}.TableGeneratorTest"
```

测试类若以 `@SpringBootTest` 起全量上下文，需带属性覆盖避免与本机运行中的实例端口冲突、避免测试实例注册进 Nacos：

```java
@SpringBootTest(properties = {
        "dubbo.protocol.port=-1",
        "dubbo.application.qos-enable=false",
        "dubbo.registry.register=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
```

**汇报端点清单**（`{groupId}` 前缀由 starter 自动添加；经网关为 `/api/{app}/{groupId}/...`）：

```
GET    /xxxs             分页列表（Grid，条件来自 query 参数）
GET    /xxxs/detail      分页列表（强类型实体）
GET    /xxxs/{id}        按 id 查
GET    /xxxs/one         按条件查单条
GET    /xxxs/new         空白实体
GET    /xxxs/codes/{code}  按 code 查（仅 BaseCodeApi）
POST   /xxxs             新增/更新（insertOrUpdate，@Valid）
PUT    /xxxs/{id}        全量更新
PATCH  /xxxs/{id}        部分更新
DELETE /xxxs/{id}        删除
```

## 禁止事项

- 不要手写 `group_id` 条件或 `setGroupId()`——多租户由 starter 的 TableDAO/回调自动处理
- 不要给实体重复声明 `id`/审计字段/`groupId`
- 不要把实体放到非 `**.entity` 包
- 不要新建 yml 配置或改启动类——现有扫描/装配已覆盖
- 不要为 CRUD 写自定义 Service/Controller 方法（超出"最基本 CRUD"的需求先问用户）
- 枚举不要只写常量名不带 `getCode()`

## 故障排查

| 症状 | 原因 |
| --- | --- |
| 建表 RuntimeException: NoSuchMethodException getCode | 枚举字段缺 `getCode()` |
| 分类查询 SQL 报错 column "category" does not exist | 分类 DAO 未显式传列名（无参构造默认 `category`），构造器 `super("site_id")` 指定实际分类列 |
| 测试上下文 NoClassDefFoundError: javax/servlet/... | classpath 解析到旧版 `sharp-fileupload:3.0-SNAPSHOT`（javax 编译），与 Spring Boot 3 不兼容；检查 `gradle :{模块}:dependencyInsight --dependency sharp-fileupload`，在 component-starter 对传递来源加 exclude |
| 测试 BindException 20880/22223 | 本机有运行中的同模块实例；用第 4 步的测试属性覆盖 |
| 接口 404 | URL 漏了 `{groupId}` 前缀（直调 `/{groupId}/xxxs`，经网关 `/api/{app}/{groupId}/xxxs`） |
