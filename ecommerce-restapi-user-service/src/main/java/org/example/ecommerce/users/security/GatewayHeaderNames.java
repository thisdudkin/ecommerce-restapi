package org.example.ecommerce.users.security;

public final class GatewayHeaderNames {

    public static final String AUTHENTICATED_USER_ID = "X-Authenticated-User-Id";
    public static final String AUTHENTICATED_USER_ROLE = "X-Authenticated-User-Role";
    public static final String AUTHENTICATED_TOKEN_TYPE = "X-Authenticated-Token-Type";

    public static final String ACCESS_TOKEN_TYPE = "ACCESS";
    public static final String INTERNAL_TOKEN_TYPE = "INTERNAL";

    public static final String INTERNAL_SERVICE_ROLE = "INTERNAL_SERVICE";

    private GatewayHeaderNames() {
        throw new AssertionError();
    }

}
