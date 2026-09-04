package com.rick.platform.module.user;

import com.rick.platform.module.user.entity.User;

/**
 * 当前请求的用户上下文。
 * 网关认证后透传 X-User-Id / X-User-Mobile，由拦截器写入，
 * 业务代码（如 DatabaseConfig 填充 create_by）可随时读取。
 */
public final class UserContextHolder {

    private static final ThreadLocal<User> CONTEXT = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void put(User user) {
        CONTEXT.set(user);
    }

    public static User get() {
        return CONTEXT.get();
    }

    public static void remove() {
        CONTEXT.remove();
    }
}
