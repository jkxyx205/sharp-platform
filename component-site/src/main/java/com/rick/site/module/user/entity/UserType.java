package com.rick.site.module.user.entity;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户类型（同时作为 User 的分类字段）。
 * sharp 框架约定：实体枚举字段必须有 getCode()（建表生成 VARCHAR(32) + CHECK IN 约束，
 * 写入/分类查询按 code 存储）；valueOfCode 供 DB 读取 / JSON 反序列化时按 code 还原。
 */
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
