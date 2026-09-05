package com.rick.gateway.security;

/**
 * 网关用户上下文：token 校验通过后的用户身份。
 * 作为自定义 mobile 解析方法（captcha.rules[].mobile 配置）的参数类型。
 */
public record User(Long userId, String mobile) {
}
