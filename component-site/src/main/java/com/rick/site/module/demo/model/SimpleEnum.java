package com.rick.site.module.demo.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.rick.common.util.EnumUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SimpleEnum {
    S("S", "stop"),
    A("A", "action");

    private final String code;

    private final String label;

    private static final Map<String, SimpleEnum> codeMap = new HashMap<>();
    static {
        for (SimpleEnum e : values()) {
            codeMap.put(e.code, e);
        }
    }

    public static SimpleEnum valueOfCode(String code) {
        /**
         * 没有实现 IEnum 接口，自己维护 valueOfCode（取代 EnumUtils.valueOfCode(CodeEnum.class, code)）
         * 方式1，查询
         * 方式2，缓存
         * 方式3，valueOfCode
         */
//        for (SimpleEnum simpleEnum : SimpleEnum.values()) {
//            if (simpleEnum.getCode().equals(code)) {
//                return simpleEnum;
//            }
//        }
//        return null;

//        return codeMap.get(code);
        return (SimpleEnum) EnumUtils.valueOfCode(SimpleEnum.class, code);
    }
}
