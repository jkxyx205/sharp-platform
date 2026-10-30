package com.rick.erp.module.common.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 特殊库存
 * @author Rick
 * @createdAt 2022-11-09 10:41:00
 */
@AllArgsConstructor
@Getter
public enum SpecialStockEnum {
    OUTSOURCE_CUSTOMER("委外客户"),
    SUPPLIER_CONSIGNMENT("供应商寄售"),
    PO("销售订单专用"),
    OUTSOURCE_SUPPLIER("委外供应商"),
    CUSTOMER_CONSIGNMENT("客户寄售"),
    IN_TRANSIT("在途"),
    LEND("借出"),
    LEASED_OUT("租出"),
    LEASED_IN("租入"),
    BORROW("借入"),
    SUBLEASE("转租"),
    TRANSFER("转借"),
    PROJECT("项目专用");

    @JsonValue
    public String getCode() {
        return this.name();
    }

    private final String label;

    public static SpecialStockEnum valueOfCode(String code) {
        return valueOf(code);
    }
}