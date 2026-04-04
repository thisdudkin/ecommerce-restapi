package org.example.ecommerce.orders.security;

public final class GatewayHeaderNames {

    public static final String AUTHENTICATED_USER_ID = "X-Authenticated-User-Id";
    public static final String AUTHENTICATED_USER_ROLE = "X-Authenticated-User-Role";
    public static final String AUTHENTICATED_TOKEN_TYPE = "X-Authenticated-Token-Type";
    public static final String ACCESS_TOKEN_TYPE = "ACCESS";

    private GatewayHeaderNames() {
        throw new AssertionError();
    }

}
