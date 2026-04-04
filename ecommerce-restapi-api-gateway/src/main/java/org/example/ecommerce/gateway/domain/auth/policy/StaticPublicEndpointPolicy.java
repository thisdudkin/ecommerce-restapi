package org.example.ecommerce.gateway.domain.auth.policy;

import java.util.Set;

public class StaticPublicEndpointPolicy implements PublicEndpointPolicy {

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/v1/auth/credentials",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        "/actuator/health"
    );

    @Override
    public boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path) || path.startsWith("/actuator/health/");
    }

}
