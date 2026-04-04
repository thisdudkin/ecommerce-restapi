package org.example.ecommerce.gateway.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class GatewayAuthenticationToken extends AbstractAuthenticationToken {

    private final GatewayPrincipal principal;
    private final String credentials;

    public GatewayAuthenticationToken(GatewayPrincipal principal, String credentials) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    @Override
    public GatewayPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

}
