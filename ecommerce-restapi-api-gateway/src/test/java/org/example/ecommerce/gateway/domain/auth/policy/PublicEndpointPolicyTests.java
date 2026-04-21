package org.example.ecommerce.gateway.domain.auth.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicEndpointPolicyTests {

    private final PublicEndpointPolicy policy = new StaticPublicEndpointPolicy();

    @Test
    void isPublicShouldReturnTrueForAuthenticationEndpoints() {
        assertTrue(policy.isPublic("/api/v1/auth/credentials"));
        assertTrue(policy.isPublic("/api/v1/auth/login"));
        assertTrue(policy.isPublic("/api/v1/auth/refresh"));
        assertTrue(policy.isPublic("/api/v1/auth/logout"));
    }

    @Test
    void isPublicShouldReturnTrueForActuatorHealthEndpoints() {
        assertTrue(policy.isPublic("/actuator/health"));
        assertTrue(policy.isPublic("/actuator/health/readiness"));
        assertTrue(policy.isPublic("/actuator/health/liveness"));
    }

    @Test
    void isPublicShouldReturnFalseForProtectedEndpoints() {
        assertFalse(policy.isPublic("/api/v1/users/1"));
        assertFalse(policy.isPublic("/api/v1/orders/1"));
        assertFalse(policy.isPublic("/api/v1/items/1"));
    }
}
