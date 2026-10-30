package com.rick.erp.module.purchasing.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 行项目分类（采购场景）
 * @author Rick
 * @createdAt 2022-11-08 15:08:00
 */
@AllArgsConstructor
@Getter
public enum ItemCategoryEnum {
    STANDARD("标准"),
    CONSIGNMENT("供应商寄售"),
    SUBCONTRACTING("委外供应商"),
    THIRD_PARTY("第三方"),
    STOCK_TRANSFER("库存调拨"),
    SERVICE("服务"),
    LEASED_IN("租入"),
    PROJECT("项目专用");

    @JsonValue
    public String getCode() {
        return this.name();
    }

    private final String label;

    public static ItemCategoryEnum valueOfCode(String code) {
        return valueOf(code);
    }
}