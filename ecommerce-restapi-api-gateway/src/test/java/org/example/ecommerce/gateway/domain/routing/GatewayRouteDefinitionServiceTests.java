package org.example.ecommerce.gateway.domain.routing;

import org.example.ecommerce.gateway.domain.routing.model.RouteSpec;
import org.example.ecommerce.gateway.domain.routing.service.GatewayRouteDefinitionService;
import org.example.ecommerce.gateway.infrastructure.config.GatewayRoutesProperties;
import org.example.ecommerce.gateway.shared.GatewayHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayRouteDefinitionServiceTests {

    @Test
    void routeSpecsShouldContainExpectedRoutesAndHeaders() {
        GatewayRoutesProperties properties = new GatewayRoutesProperties(
            "http://user-service:8081",
            "http://auth-service:8082",
            "http://order-service:8083",
            "http://payment-service:8084"
        );

        GatewayRouteDefinitionService service = new GatewayRouteDefinitionService(properties);

        List<RouteSpec> routes = service.routeSpecs();

        assertEquals(5, routes.size());

        RouteSpec authRoute = findById(routes, "auth-service");
        assertEquals("http://auth-service:8082", authRoute.uri());
        assertTrue(authRoute.paths().contains("/api/v1/auth/credentials"));
        assertTrue(authRoute.paths().contains("/api/v1/auth/login"));
        assertTrue(authRoute.paths().contains("/api/v1/auth/refresh"));
        assertTrue(authRoute.paths().contains("/api/v1/auth/logout"));
        assertTrue(authRoute.headersToRemove().contains(HttpHeaders.COOKIE));
        assertTrue(authRoute.headersToRemove().contains(GatewayHeaders.AUTHENTICATED_USER_ID));
        assertTrue(authRoute.headersToRemove().contains(GatewayHeaders.AUTHENTICATED_USER_ROLE));
        assertTrue(authRoute.headersToRemove().contains(GatewayHeaders.AUTHENTICATED_TOKEN_TYPE));

        RouteSpec userRoute = findById(routes, "user-service");
        assertEquals("http://user-service:8081", userRoute.uri());
        assertTrue(userRoute.paths().contains("/api/v1/users/**"));
        assertTrue(userRoute.headersToRemove().contains(HttpHeaders.COOKIE));
        assertTrue(userRoute.headersToRemove().contains(HttpHeaders.AUTHORIZATION));

        RouteSpec ordersRoute = findById(routes, "order-service-orders");
        assertEquals("http://order-service:8083", ordersRoute.uri());
        assertTrue(ordersRoute.paths().contains("/api/v1/orders/**"));
        assertTrue(ordersRoute.headersToRemove().contains(HttpHeaders.COOKIE));
        assertTrue(ordersRoute.headersToRemove().contains(HttpHeaders.AUTHORIZATION));

        RouteSpec itemsRoute = findById(routes, "order-service-items");
        assertEquals("http://order-service:8083", itemsRoute.uri());
        assertTrue(itemsRoute.paths().contains("/api/v1/items/**"));
        assertTrue(itemsRoute.headersToRemove().contains(HttpHeaders.COOKIE));
        assertTrue(itemsRoute.headersToRemove().contains(HttpHeaders.AUTHORIZATION));
    }

    private RouteSpec findById(List<RouteSpec> routes, String id) {
        return routes.stream()
            .filter(route -> route.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Route not found: " + id));
    }

}
