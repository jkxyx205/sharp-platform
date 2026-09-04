package com.rick.platform.module.user.exception;

import org.springframework.http.HttpStatus;

/**
 * 注册/登录业务异常：status 决定 HTTP 响应码，message 直接返回给客户端。
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
