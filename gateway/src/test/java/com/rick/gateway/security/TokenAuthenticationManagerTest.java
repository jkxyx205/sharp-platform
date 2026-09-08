package com.rick.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenAuthenticationManagerTest {

    private TokenStore tokenStore;
    private TokenAuthenticationManager manager;

    @BeforeEach
    void setUp() {
        tokenStore = new TokenStore();
        manager = new TokenAuthenticationManager(tokenStore);
    }

    @Test
    void validTokenGetsStoredPermissionsAsAuthorities() {
        String token = tokenStore.create(1L, "13800000000", List.of("user", "admin"));

        Authentication auth = manager.authenticate(new ApiTokenAuthentication(token)).block();

        assertEquals("13800000000", auth.getPrincipal());
        assertTrue(auth.isAuthenticated());
        List<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
        assertEquals(List.of("user", "admin"), authorities);
    }

    @Test
    void unknownTokenRejected() {
        assertThrows(BadCredentialsException.class,
                () -> manager.authenticate(new ApiTokenAuthentication("no-such-token")).block());
    }

    @Test
    void removedTokenRejected() {
        String token = tokenStore.create(2L, "13900000000", List.of("user"));
        tokenStore.remove(token);
        assertThrows(BadCredentialsException.class,
                () -> manager.authenticate(new ApiTokenAuthentication(token)).block());
    }
}
