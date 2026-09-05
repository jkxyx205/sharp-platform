package com.rick.gateway.captcha;

/**
 * 验证码种类。
 */
public enum CodeKind {

    /** 图片验证码：字母数字，校验不区分大小写 */
    IMAGE,

    /** 短信验证码：数字 */
    SMS
}
