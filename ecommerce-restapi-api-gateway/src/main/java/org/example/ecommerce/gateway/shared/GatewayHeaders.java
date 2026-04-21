package org.example.ecommerce.gateway.shared;

public final class GatewayHeaders {

    public static final String AUTHENTICATED_USER_ID = "X-Authenticated-User-Id";
    public static final String AUTHENTICATED_USER_ROLE = "X-Authenticated-User-Role";
    public static final String AUTHENTICATED_TOKEN_TYPE = "X-Authenticated-Token-Type";

    private GatewayHeaders() {
    }

}
