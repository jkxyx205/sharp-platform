package com.rick.site.module.demo.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.rick.common.util.EnumUtils;
import com.rick.common.util.model.IEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Getter
public enum CodeIntegerEnum implements IEnum<Integer> {
    F(0, "Start"),
    M(1, "End");

    @JsonValue
    private final Integer code;

    private final String label;

    public static CodeIntegerEnum valueOfCode(Integer code) {
        return EnumUtils.fromCode(CodeIntegerEnum.class, code);
    }

    /**
     * 前端传值是字符串
     * @param code
     * @return
     */
    public static CodeIntegerEnum valueOfCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return valueOfCode(Integer.valueOf(code));
    }
}
