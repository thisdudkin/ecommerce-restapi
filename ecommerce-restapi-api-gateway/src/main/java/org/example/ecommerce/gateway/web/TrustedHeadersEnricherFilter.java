package org.example.ecommerce.gateway.web;

import org.example.ecommerce.gateway.security.GatewayHeaderNames;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TrustedHeadersEnricherFilter implements GlobalFilter, Ordered {

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,
                             @NonNull GatewayFilterChain chain) {
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(exchange.getRequest().mutate()
                .headers(httpHeaders -> {
                    httpHeaders.remove("Forwarded");
                    httpHeaders.remove("X-Forwarded-For");
                    httpHeaders.remove("X-Forwarded-Host");
                    httpHeaders.remove("X-Forwarded-Port");
                    httpHeaders.remove("X-Forwarded-Proto");
                })
                .build())
            .build();

        return chain.filter(mutatedExchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
