package com.rick.gateway.captcha;

import com.rick.gateway.security.User;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 自定义 mobile 解析方法：启动期编译「全限定类名.方法名」，运行期反射调用。
 * <p>
 * 方法签名固定为 {@code String xxx(User user, String type)}；
 * 支持静态方法；实例方法优先取 Spring Bean，否则按无参构造实例化。
 */
public final class MobileResolver {

    private final Method method;
    private final Object target;

    private MobileResolver(Method method, Object target) {
        this.method = method;
        this.target = target;
    }

    public static MobileResolver compile(String ref, ApplicationContext context) {
        int idx = ref.lastIndexOf('.');
        if (idx <= 0 || idx == ref.length() - 1) {
            throw new IllegalArgumentException("mobile 方法引用格式错误（应为 全限定类名.方法名）: " + ref);
        }
        String className = ref.substring(0, idx);
        String methodName = ref.substring(idx + 1);
        try {
            Class<?> clazz = ClassUtils.forName(className, MobileResolver.class.getClassLoader());
            Method method = clazz.getMethod(methodName, User.class, String.class);
            Object target = Modifier.isStatic(method.getModifiers()) ? null : instance(clazz, context, ref);
            return new MobileResolver(method, target);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("mobile 方法引用的类不存在: " + ref, e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "mobile 方法引用不存在（签名须为 String " + methodName + "(User, String)）: " + ref, e);
        }
    }

    private static Object instance(Class<?> clazz, ApplicationContext context, String ref) {
        try {
            return context.getBean(clazz);
        } catch (RuntimeException e) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException ex) {
                throw new IllegalArgumentException(
                        "mobile 方法引用为实例方法且无法实例化（需为 Spring Bean 或有无参构造）: " + ref, ex);
            }
        }
    }

    public String resolve(User user, String type) {
        try {
            return (String) method.invoke(target, user, type);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("mobile 解析方法调用失败: " + method, e);
        }
    }
}
