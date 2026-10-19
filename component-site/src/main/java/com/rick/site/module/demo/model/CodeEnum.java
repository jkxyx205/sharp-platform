package com.rick.site.module.demo.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.rick.common.util.EnumUtils;
import com.rick.common.util.model.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CodeEnum implements IEnum<String> {
    F("Fe", "Female"),
    M("Ma", "Male"),
    UNKNOWN("", "UNKNOWN");

    @JsonValue
    private final String code;

    private final String label;

    public static CodeEnum valueOfCode(String code) {
        return EnumUtils.fromCode(CodeEnum.class, code);
    }
}
