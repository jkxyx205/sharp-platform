package com.rick.site.module.demo.model;

import com.rick.common.util.model.IEnum;

public enum NameEnum implements IEnum<String> {
    A, B;

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public String getLabel() {
        return name();
    }

    public static NameEnum valueOfCode(String code) {
        return valueOf(code);
    }
}
