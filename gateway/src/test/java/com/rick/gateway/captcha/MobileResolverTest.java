package com.rick.gateway.captcha;

import com.rick.gateway.security.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MobileResolverTest {

    public static class StaticResolver {
        public static String getMobile(User user, String type) {
            return "17700000001-" + (user == null ? "anon" : user.mobile()) + "-" + type;
        }
    }

    public static class InstanceResolver {
        public String getMobile(User user, String type) {
            return "17700000002";
        }
    }

    public static class NoArgCtorResolver {
        public String getMobile(User user, String type) {
            return "17700000003";
        }
    }

    private ApplicationContext context;

    @BeforeEach
    void setUp() {
        context = mock(ApplicationContext.class);
    }

    @Test
    void staticMethodDoesNotTouchContext() {
        MobileResolver resolver = MobileResolver.compile(StaticResolver.class.getName() + ".getMobile", context);
        assertEquals("17700000001-anon-register", resolver.resolve(null, "register"));
        assertEquals("17700000001-13800000000-login", resolver.resolve(new User(1L, "13800000000"), "login"));
        verifyNoInteractions(context);
    }

    @Test
    void instanceMethodPrefersSpringBean() {
        when(context.getBean(InstanceResolver.class)).thenReturn(new InstanceResolver());
        MobileResolver resolver = MobileResolver.compile(InstanceResolver.class.getName() + ".getMobile", context);
        assertEquals("17700000002", resolver.resolve(null, "register"));
    }

    @Test
    void instanceMethodFallsBackToNoArgCtor() {
        when(context.getBean(NoArgCtorResolver.class))
                .thenThrow(new NoSuchBeanDefinitionException(NoArgCtorResolver.class));
        MobileResolver resolver = MobileResolver.compile(NoArgCtorResolver.class.getName() + ".getMobile", context);
        assertEquals("17700000003", resolver.resolve(null, "register"));
    }

    @Test
    void invalidRefsFailFast() {
        assertThrows(IllegalArgumentException.class, () -> MobileResolver.compile("NoDotsHere", context));
        assertThrows(IllegalArgumentException.class, () -> MobileResolver.compile("com.no.such.Clazz.m", context));
        assertThrows(IllegalArgumentException.class,
                () -> MobileResolver.compile(StaticResolver.class.getName() + ".notExist", context));
    }
}
