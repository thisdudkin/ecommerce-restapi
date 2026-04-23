package org.example.ecommerce.gateway.infrastructure.config;

import org.example.ecommerce.gateway.domain.routing.service.GatewayRouteDefinitionService;
import org.example.ecommerce.gateway.domain.routing.model.RouteSpec;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator gatewayRouteLocator(RouteLocatorBuilder builder,
                                            GatewayRouteDefinitionService routeDefinitionService,
                                            GatewayRoutesProperties properties) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        for (RouteSpec routeSpec : routeDefinitionService.routeSpecs()) {
            routes.route(routeSpec.id(), route -> route
                .path(routeSpec.paths().toArray(String[]::new))
                .filters(filter -> {
                    for (String header : routeSpec.headersToRemove()) {
                        filter.removeRequestHeader(header);
                    }
                    return filter.preserveHostHeader();
                })
                .uri(routeSpec.uri())
            );
        }

        addOpenApiRoute(routes, "auth-service-openapi", "/swagger/auth/v3/api-docs", properties.authServiceUri());
        addOpenApiRoute(routes, "user-service-openapi", "/swagger/users/v3/api-docs", properties.userServiceUri());
        addOpenApiRoute(routes, "order-service-openapi", "/swagger/orders/v3/api-docs", properties.orderServiceUri());
        addOpenApiRoute(routes, "payment-service-openapi", "/swagger/payments/v3/api-docs", properties.paymentServiceUri());

        return routes.build();
    }

    private void addOpenApiRoute(RouteLocatorBuilder.Builder routes,
                                        String routeId,
                                        String externalPath,
                                        String targetUri) {
        routes.route(routeId, route -> route
            .path(externalPath)
            .filters(filter -> filter
                .setPath("/v3/api-docs")
                .removeRequestHeader(HttpHeaders.COOKIE)
                .removeRequestHeader(HttpHeaders.AUTHORIZATION)
            )
            .uri(targetUri)
        );
    }

}
