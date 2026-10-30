package com.rick.erp.module.common.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Rick
 * @createdAt 2022-04-30 11:01:00
 */
@AllArgsConstructor
@Getter
public enum CurrencyEnum {
    CNY("人民币"),
    USD("美元");

    @JsonValue
    public String getCode() {
        return this.name();
    }

    private final String label;

    public static CurrencyEnum valueOfCode(String code) {
        return valueOf(code);
    }
}