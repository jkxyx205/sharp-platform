package com.rick.erp.module.common.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存类型
 * @author Rick
 * @createdAt 2022-11-09 10:41:00
 */
@AllArgsConstructor
@Getter
public enum StockTypeEnum {
    UNRESTRICTED("自由库存"),
    BATCH_RESTRICTED("批次限制"),
    BLOCK("BLOCK库存");

    @JsonValue
    public String getCode() {
        return this.name();
    }

    private final String label;

    public static StockTypeEnum valueOfCode(String code) {
        return valueOf(code);
    }
}