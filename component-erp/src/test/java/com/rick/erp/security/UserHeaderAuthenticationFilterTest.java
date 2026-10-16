package com.rick.erp.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

class UserHeaderAuthenticationFilterTest {

    private final UserHeaderAuthenticationFilter filter = new UserHeaderAuthenticationFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userId1GetsHardcodedPlantCurrentDataAuthority() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "1");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertEquals("1", auth.getPrincipal());
        assertTrue(auth.getAuthorities().contains(new SimpleGrantedAuthority("plant:current_data")));
    }

    @Test
    void otherUserIdAuthenticatedWithoutAuthorities() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "2");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.isAuthenticated());
        assertTrue(auth.getAuthorities().isEmpty());
    }

    @Test
    void missingHeaderLeavesSecurityContextEmpty() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
