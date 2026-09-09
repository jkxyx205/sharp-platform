package com.rick.gateway.captcha.resolver;

import com.rick.gateway.captcha.MobileResolver;
import com.rick.gateway.security.User;
import org.springframework.stereotype.Component;

/**
 * 注册规则的 mobile 解析示例：直接透传 query 参数中的 mobile
 * （注册为匿名请求，user 为 null）。yml 中按 bean 名引用：
 * {@code captcha.rules[].mobile: registerMobileResolver}
 */
@Component
public class RegisterMobileResolver implements MobileResolver {

    @Override
    public String getMobile(User user, String mobile, String type) {
        return mobile;
    }
}
