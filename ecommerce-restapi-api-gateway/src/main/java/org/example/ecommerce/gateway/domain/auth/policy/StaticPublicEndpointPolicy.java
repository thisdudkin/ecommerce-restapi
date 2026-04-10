package org.example.ecommerce.gateway.domain.auth.policy;

import java.util.Set;

public class StaticPublicEndpointPolicy implements PublicEndpointPolicy {

    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/v1/auth/credentials",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/logout",
        "/actuator/health",
        "/actuator/health/**",
        "/swagger",
        "/swagger/**",
        "/swagger-ui/**",
        "/v3/api-docs/swagger-config"
    );

    @Override
    public boolean isPublic(String path) {
        return path.equals("/api/v1/auth/credentials")
            || path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/refresh")
            || path.equals("/api/v1/auth/logout")
            || path.equals("/actuator/health")
            || path.startsWith("/actuator/health/")
            || path.equals("/swagger")
            || path.startsWith("/swagger/")
            || path.startsWith("/swagger-ui/")
            || path.equals("/v3/api-docs/swagger-config");
    }

    @Override
    public String[] pathPatterns() {
        return PUBLIC_PATHS.toArray(String[]::new);
    }

}
