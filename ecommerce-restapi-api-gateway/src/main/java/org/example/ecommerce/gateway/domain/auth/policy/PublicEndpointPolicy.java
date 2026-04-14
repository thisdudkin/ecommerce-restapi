package org.example.ecommerce.gateway.domain.auth.policy;

public interface PublicEndpointPolicy {
    boolean isPublic(String path);
    String[] pathPatterns();
}
