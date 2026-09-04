package com.rick.common.component.starter.exception;

import com.rick.common.http.exception.ExceptionCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ExceptionCodeEnum implements ExceptionCode {
    PARAM_ERROR(400, "参数传递错误"),
    CODE_NOT_EXISTS_ERROR(400, "%s code「%s」不存在"),
    DUPLICATE_CODS_ERROR(400, "%s code「%s」不能重复"),
    DUPLICATE_DATA_ERROR(400, "「%s」出现重复数据"),
    CODE_EXISTS_ERROR(400, "%s code「%s」已经存在"),
    CODES_NOT_EXISTS_ERROR(400, "%s code「%s」不存在"),
    REQUIRED_ERROR(400, "「%s」必填"),
    CHECK_NOT_VALID_ERROR(500, "%s"),
    RESOURCE_NOT_EXISTS_ERROR(4004, "「%s」不存在");

    private int code;
    private String message;

    ExceptionCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}