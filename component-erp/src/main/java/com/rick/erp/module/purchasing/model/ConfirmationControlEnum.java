package com.rick.erp.module.purchasing.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Rick
 * @createdAt 2022-04-09 14:08:00
 */
@AllArgsConstructor
@Getter
public enum ConfirmationControlEnum {
    NONE("NONE"),
    INBOUND_DELIVERY("0004 inbound_delivery");

    @JsonValue
    public String getCode() {
        return this.name();
    }

    private final String label;

    public static ConfirmationControlEnum valueOfCode(String code) {
        return valueOf(code);
    }
}