package com.rick.gateway.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * 请求头中携带的待校验 token。
 */
public class ApiTokenAuthentication extends AbstractAuthenticationToken {

    private final String token;

    public ApiTokenAuthentication(String token) {
        super(null);
        this.token = token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }
}
