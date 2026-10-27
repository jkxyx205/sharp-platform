package com.rick.erp.module.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rick.common.util.model.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum PresetTypeEnum implements IEnum<String> {
    PRESET("系统预设"),
    CUSTOMIZE("客户自定义");

    private final String label;

    @Override
    public String getCode() {
        return name();
    }

    public static PresetTypeEnum valueOfCode(String code) {
        return valueOf(code);
    }
}