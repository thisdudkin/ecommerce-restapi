package org.example.ecommerce.gateway.web.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

public class GatewayAuthenticationToken extends AbstractAuthenticationToken {

    private final GatewayPrincipal principal;
    private final String credentials;

    public GatewayAuthenticationToken(GatewayPrincipal principal, String credentials) {
        super(AuthorityUtils.createAuthorityList("ROLE_" + principal.role()));
        this.principal = principal;
        this.credentials = credentials;
        setAuthenticated(true);
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    @Override
    public GatewayPrincipal getPrincipal() {
        return principal;
    }

}
