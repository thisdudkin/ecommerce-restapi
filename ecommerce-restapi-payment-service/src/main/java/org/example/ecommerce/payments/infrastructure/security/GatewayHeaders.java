package org.example.ecommerce.payments.infrastructure.security;

import lombok.experimental.UtilityClass;

@UtilityClass
public class GatewayHeaders {

    public static final String AUTHENTICATED_USER_ID = "X-Authenticated-User-Id";
    public static final String AUTHENTICATED_USER_ROLE = "X-Authenticated-User-Role";
    public static final String AUTHENTICATED_TOKEN_TYPE = "X-Authenticated-Token-Type";
    public static final String ACCESS_TOKEN_TYPE = "ACCESS";

}
