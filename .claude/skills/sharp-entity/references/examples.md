# sharp-entity 完整示例

> 四个典型场景的完整可复制示例，均按 skill 流程实际生成并验证过（编译 + 建表）。
> 代码库内活参考：`component-site` 的 `com.rick.site.module.user`（场景 4）、`com.rick.site.module.demo` 的 Plant（code+description 链）。
> 场景 1–3 的 Site/Role/Page 曾在 component-site 落地、后已清理，完整代码以本文件为准。
>
> 枚举统一采用新约定：`getCode()` 返回 `name()`（结构上保证 code = name，与建表 CHECK IN、分类存取、JSON 序列化天然一致），中文放 `label`，`@JsonValue` 使 JSON 输出即 code。

---

## 场景 1：标准实体（code + 枚举字段）— Site

**输入提示词**：在模块 component-site 下，包 com.rick.site.module.site 创建实体 Site，字段：code、name、status（NEW 新建 / ACTIVE 激活）

**决策**：有 code、无 description → `ComponentBaseCodeEntity` + `EntityCodeDAOImpl` + `BaseCodeServiceImpl` + `BaseCodeApi`

### entity/Site.java

```java
package com.rick.site.module.site.entity;

import com.rick.common.component.starter.model.ComponentBaseCodeEntity;
import com.rick.db.repository.Column;
import com.rick.db.repository.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * 站点。
 * 继承 ComponentBaseCodeEntity：自带 id、code、groupId（多租户，保存自动回填）
 * 及 create_by/create_time/update_by/update_time/is_deleted 审计字段。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "site_site", comment = "站点")
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

### entity/SiteStatus.java

```java
package com.rick.site.module.site.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 站点状态。
 * sharp 框架约定：实体枚举字段必须有 getCode()（建表生成 VARCHAR(32) + CHECK IN 约束）；
 * getCode() 返回 name() 保证 code = name；valueOfCode 供 DB 读取 / JSON 反序列化还原。
 */
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

### dao/SiteDAO.java

```java
package com.rick.site.module.site.dao;

import com.rick.db.repository.EntityCodeDAOImpl;
import com.rick.site.module.site.entity.Site;
import org.springframework.stereotype.Repository;

@Repository
public class SiteDAO extends EntityCodeDAOImpl<Site, Long> {

}
```

### service/SiteService.java

```java
package com.rick.site.module.site.service;

import com.rick.db.plugin.BaseCodeServiceImpl;
import com.rick.site.module.site.dao.SiteDAO;
import com.rick.site.module.site.entity.Site;
import org.springframework.stereotype.Service;

@Service
public class SiteService extends BaseCodeServiceImpl<SiteDAO, Site, Long> {

    public SiteService(SiteDAO siteDAO) {
        super(siteDAO);
    }

}
```

### controler/SiteController.java

```java
package com.rick.site.module.site.controler;

import com.rick.common.component.starter.controller.BaseCodeApi;
import com.rick.site.module.site.entity.Site;
import com.rick.site.module.site.service.SiteService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站点 CRUD。
 * 继承 BaseCodeApi 即获得标准端点（实际路径均带 /{groupId} 前缀，经网关为 /api/site/{groupId}/sites/...）：
 * GET    ""              分页列表（Grid）
 * GET    "detail"        分页列表（强类型实体）
 * GET    "one"           按条件查单条
 * GET    "new"           空白实体
 * GET    "{id}"          按 id 查
 * GET    "codes/{code}"  按 code 查
 * POST   ""              新增/更新（insertOrUpdate）
 * PUT    "{id}"          全量更新
 * PATCH  "{id}"          部分更新
 * DELETE "{id}"          删除
 */
@RestController
@RequestMapping("sites")
public class SiteController extends BaseCodeApi<SiteService, Site, Long> {

    public SiteController(SiteService baseService) {
        super(baseService);
    }

}
```

### API 访问示例

```bash
# 新增（经网关，{groupId}=1；status 传枚举 name）
curl -X POST 'http://localhost:8769/api/site/1/sites' \
  -H 'Authorization: Bearer {token}' -H 'Content-Type: application/json' \
  -d '{"code": "S001", "name": "总部站点", "status": "NEW"}'

# 分页列表（query 参数即查询条件）
curl 'http://localhost:8769/api/site/1/sites?code=S001&page=1&rows=20' \
  -H 'Authorization: Bearer {token}'

# 按 code 查
curl 'http://localhost:8769/api/site/1/sites/codes/S001' -H 'Authorization: Bearer {token}'
```

建表 DDL 关键列（自动生成）：`code VARCHAR(32)`、`name VARCHAR(32)`、`status VARCHAR(32) CHECK (status IN ('NEW','ACTIVE'))`、`group_id BIGINT`。

---

## 场景 2：code + description + JSON Map 字段 — Role

**输入提示词**：包 com.rick.site.module.role 创建实体 Role，字段：code、name、description、info（Map<String, Object>）

**决策**：有 code + description → `ComponentBaseCodeDescriptionEntity` 链；**description 基类自带，不重复声明**；Map 字段用 `columnDefinition = "json"`

只列与场景 1 的差异（DAO/Service/Controller 同构，基类链同场景 1，路由 `roles`）：

### entity/Role.java

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "site_role", comment = "角色")
public class Role extends ComponentBaseCodeDescriptionEntity<Long> {

    @NotBlank
    String code;

    @NotBlank
    @Column(comment = "角色名称")
    String name;

    @Column(columnDefinition = "json", comment = "扩展信息")
    Map<String, Object> info;

}
```

- `info` 建为 PG `json` 列；读写由框架 `dbConversionService`（`JsonStringToMapConverterFactory`）自动 Map ↔ JSON 转换
- `BaseApi` 的 list 端点输出时 PGobject 自动转 `JsonNode`
- 请求体示例：`{"code": "admin", "name": "管理员", "description": "内置角色", "info": {"level": 1, "tags": ["a"]}}`

---

## 场景 3：动态值分类字段（Long）— Page

**输入提示词**：包 com.rick.site.module.page 创建实体 Page，字段：name、description、siteId；siteId 作为分类字段

**决策**：无 code → `ComponentBaseEntity` 链（description 作普通字段声明）；分类字段 Long → 实体实现 `RowCategory<Long>` + DAO 用 `CategoryEntityDAOImpl`

### entity/Page.java（差异部分）

```java
@Table(value = "site_page", comment = "页面")
public class Page extends ComponentBaseEntity<Long> implements RowCategory<Long> {

    @NotBlank
    @Column(comment = "页面名称")
    String name;

    @Column(comment = "描述")
    String description;

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
```

### dao/PageDAO.java

```java
@Repository
public class PageDAO extends CategoryEntityDAOImpl<Page, Long, Long> {

    public PageDAO() {
        super("site_id");   // ⚠️ 分类字段的 DB 列名（snake_case），无参构造默认 "category"
    }

}
```

Service/Controller 同场景 1 结构（`BaseServiceImpl` + `BaseApi`，路由 `pages`）。分类能力：`pageDAO.selectAll(siteId)`、`pageDAO.insertOrUpdate(siteId, pages)`（整组替换）。

---

## 场景 4：枚举分类字段 — User（活代码：com.rick.site.module.user）

**输入提示词**：包 com.rick.site.module.user 创建实体 User（表名 sys_user），字段：description、idCard(String)、score(Short)、remark(text)、type（枚举 ADMIN/USER）；type 作为分类字段

**决策**：无 code → `ComponentBaseEntity` 链；分类字段是枚举 → `CategoryEnumEntityDAOImpl`（DB 按枚举 code 存取，与建表 CHECK IN / 实体写入 / valueOfCode 全链一致；枚举模板 getCode() 返回 name()，天然满足 code = name）

### entity/UserType.java

```java
@AllArgsConstructor
@Getter
public enum UserType {
    ADMIN("管理员"),
    USER("用户");

    @JsonValue
    public String getCode() {
        return this.name();
    }

    private final String label;

    public static UserType valueOfCode(String code) {
        return valueOf(code);
    }
}
```

### entity/User.java（差异部分）

```java
@Table(value = "sys_user", comment = "用户")
public class User extends ComponentBaseEntity<Long> implements RowCategory<UserType> {

    @Column(comment = "描述")
    String description;

    @Column(comment = "身份证号")
    String idCard;

    @Column(comment = "分数")
    Short score;                                  // → SMALLINT

    @Column(columnDefinition = "text", comment = "备注")
    String remark;                                // → TEXT

    @Column(comment = "类型")
    UserType type;                                // → VARCHAR(32) CHECK (type IN ('ADMIN','USER'))

    @Override
    @JsonIgnore   // 避免 JSON 输出与 type 重复
    public UserType getCategory() {
        return type;
    }

    @Override
    public void setCategory(UserType category) {
        this.type = category;
    }
}
```

### dao/UserDAO.java

```java
@Repository
public class UserDAO extends CategoryEnumEntityDAOImpl<User, Long, UserType> {

    public UserDAO() {
        super("type");   // ⚠️ 传分类字段的 DB 列名，不是类型名 "UserType"，也不是默认 "category"
    }

}
```

分类能力：`userDAO.selectAll(UserType.ADMIN)`、`userDAO.insertOrUpdate(UserType.ADMIN, users)`（整组替换）。

---

## 建表测试（所有场景通用）

`src/test/java/com/rick/{app}/TableGeneratorTest.java`——createTable 非幂等，**按实体拆方法**，用 `--tests` 单方法执行：

```java
// 自足运行：随机 Dubbo 端口 + 关闭 qos，避免与本机运行中的实例冲突；
// 不向 Nacos 注册测试实例，防止网关把流量路由到测试进程
@SpringBootTest(properties = {
        "dubbo.protocol.port=-1",
        "dubbo.application.qos-enable=false",
        "dubbo.registry.register=false",
        "spring.cloud.nacos.discovery.register-enabled=false"
})
public class TableGeneratorTest {

    @Autowired
    TableGenerator tableGenerator;

    @Test
    public void testGeneratorUserTable() {
        tableGenerator.createTable(User.class);
    }
}
```

```bash
export JAVA_HOME="/Applications/IntelliJ IDEA.app/Contents/jbr/Contents/Home"
gradle -p /Users/rick/Space/Workspace/sharp-platform :component-site:test \
  --tests "com.rick.site.TableGeneratorTest.testGeneratorUserTable"
```
