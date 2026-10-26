package com.rick.demo.module.common.exception;

import com.rick.common.http.exception.ExceptionCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum ExceptionCodeEnum implements ExceptionCode {
    DEMO_EXPIRED(7001001, "DEMO 已经过期！");

    private int code;
    private String message;

    ExceptionCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }
}