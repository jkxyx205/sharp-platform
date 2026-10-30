package com.rick.erp.module.common.model;

import com.rick.common.util.model.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BoundTypeEnum implements IEnum<String> {
    INBOUND("入库"),
    OUTBOUND("出库");

    public String getCode() {
        return this.name();
    }

    private final String label;

    public static BoundTypeEnum valueOfCode(String code) {
        return valueOf(code);
    }
}