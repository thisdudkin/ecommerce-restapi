package org.example.ecommerce.gateway.config;

import org.example.ecommerce.gateway.security.GatewayHeaderNames;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator gatewayRouteLocator(RouteLocatorBuilder builder,
                                            GatewayRoutesProperties properties) {
        return builder.routes()

            .route("auth-service", route -> route
                .path(
                    "/api/v1/auth/credentials",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/logout"
                )
                .filters(filter -> filter
                    .removeRequestHeader(HttpHeaders.COOKIE)
                    .removeRequestHeader(GatewayHeaderNames.AUTHENTICATED_USER_ID)
                    .removeRequestHeader(GatewayHeaderNames.AUTHENTICATED_USER_ROLE)
                    .removeRequestHeader(GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE)
                    .preserveHostHeader()
                )
                .uri(properties.authServiceUri())
            )

            .route("user-service", route -> route
                .path("/api/v1/users/**")
                .filters(filter -> filter
                    .removeRequestHeader(HttpHeaders.COOKIE)
                    .removeRequestHeader(HttpHeaders.AUTHORIZATION)
                    .preserveHostHeader()
                )
                .uri(properties.userServiceUri())
            )

            .route("order-service-orders", route -> route
                .path("/api/v1/orders/**")
                .filters(filter -> filter
                    .removeRequestHeader(HttpHeaders.COOKIE)
                    .removeRequestHeader(HttpHeaders.AUTHORIZATION)
                    .preserveHostHeader()
                )
                .uri(properties.orderServiceUri())
            )

            .route("order-service-items", route -> route
                .path("/api/v1/items/**")
                .filters(filter -> filter
                    .removeRequestHeader(HttpHeaders.COOKIE)
                    .removeRequestHeader(HttpHeaders.AUTHORIZATION)
                    .preserveHostHeader()
                )
                .uri(properties.orderServiceUri())
            )

            .build();
    }

}
