package org.example.ecommerce.gateway.infrastructure.config;

import org.example.ecommerce.gateway.domain.routing.service.GatewayRouteDefinitionService;
import org.example.ecommerce.gateway.domain.routing.model.RouteSpec;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator gatewayRouteLocator(RouteLocatorBuilder builder,
                                            GatewayRouteDefinitionService routeDefinitionService) {
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

        return routes.build();
    }

}
