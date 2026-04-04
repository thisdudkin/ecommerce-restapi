package org.example.ecommerce.gateway.security;

public final class GatewayHeaderNames {

    public static final String AUTHENTICATED_USER_ID = "X-Authenticated-User-Id";
    public static final String AUTHENTICATED_USER_ROLE = "X-Authenticated-User-Role";
    public static final String AUTHENTICATED_TOKEN_TYPE = "X-Authenticated-Token-Type";
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private GatewayHeaderNames() {
    }

}
