package com.rick.site.module.user.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rick.common.component.starter.model.ComponentBaseEntity;
import com.rick.db.repository.Column;
import com.rick.db.repository.Table;
import com.rick.db.repository.support.category.RowCategory;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * 用户（site 侧，表 sys_user，与 platform 库的 sys_user 不同库不冲突）。
 * 继承 ComponentBaseEntity：自带 id、groupId（多租户，保存自动回填）及审计字段；
 * 无 code，description 作为普通字段声明。
 * 实现 RowCategory：type 作为分类字段，供 CategoryEntityDAOImpl 按类型归类
 * （selectAll(UserType.ADMIN) / insertOrUpdate(UserType.ADMIN, users)）。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Table(value = "sys_user", comment = "用户")
public class User extends ComponentBaseEntity<Long> implements RowCategory<UserType> {

    @Column(comment = "描述")
    String description;

    @Column(comment = "身份证号")
    String idCard;

    @Column(comment = "分数")
    Short score;

    @Column(columnDefinition = "text", comment = "备注")
    String remark;

    @Column(comment = "类型")
    UserType type;

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
