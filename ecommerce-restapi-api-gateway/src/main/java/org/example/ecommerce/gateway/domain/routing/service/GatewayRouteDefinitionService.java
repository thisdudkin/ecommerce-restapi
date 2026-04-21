package org.example.ecommerce.gateway.domain.routing.service;

import org.example.ecommerce.gateway.domain.routing.model.RouteSpec;
import org.example.ecommerce.gateway.infrastructure.config.GatewayRoutesProperties;
import org.example.ecommerce.gateway.shared.GatewayHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GatewayRouteDefinitionService {

    private final GatewayRoutesProperties properties;

    public GatewayRouteDefinitionService(GatewayRoutesProperties properties) {
        this.properties = properties;
    }

    public List<RouteSpec> routeSpecs() {
        return List.of(
            new RouteSpec(
                "auth-service",
                List.of(
                    "/api/v1/auth/credentials",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout"
                ),
                properties.authServiceUri(),
                List.of(
                    HttpHeaders.COOKIE,
                    GatewayHeaders.AUTHENTICATED_USER_ID,
                    GatewayHeaders.AUTHENTICATED_USER_ROLE,
                    GatewayHeaders.AUTHENTICATED_TOKEN_TYPE
                )
            ),
            new RouteSpec(
                "user-service",
                List.of("/api/v1/users/**"),
                properties.userServiceUri(),
                List.of(
                    HttpHeaders.COOKIE,
                    HttpHeaders.AUTHORIZATION
                )
            ),
            new RouteSpec(
                "order-service-orders",
                List.of("/api/v1/orders/**"),
                properties.orderServiceUri(),
                List.of(
                    HttpHeaders.COOKIE,
                    HttpHeaders.AUTHORIZATION
                )
            ),
            new RouteSpec(
                "order-service-items",
                List.of("/api/v1/items/**"),
                properties.orderServiceUri(),
                List.of(
                    HttpHeaders.COOKIE,
                    HttpHeaders.AUTHORIZATION
                )
            )
        );
    }

}
