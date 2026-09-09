package com.rick.gateway.captcha;

import com.rick.gateway.security.User;

/**
 * 自定义 mobile 解析：在 captcha.rules[].mobile 中按 Spring Bean 名配置（参照 pre-handler），
 * 由 {@link ValidateCodeFilter} 在校验短信验证码时优先调用。
 * <p>
 * 解析顺序：自定义 resolver → token 用户上下文 → query 参数兜底；
 * 返回空则继续走后续兜底。
 */
public interface MobileResolver {

    /**
     * @param user   token 用户上下文，匿名请求为 null
     * @param mobile 请求 query 参数中的原始 mobile，可能为 null
     * @param type   规则的业务类型（captcha.rules[].type，默认 url 末段）
     * @return 用于验证码比对的手机号；返回空则回退默认解析链
     */
    String getMobile(User user, String mobile, String type);
}
