package org.example.ecommerce.gateway.web;

import org.example.ecommerce.gateway.security.GatewayHeaderNames;
import org.jspecify.annotations.NonNull;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdWebFilter implements WebFilter {

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange,
                             @NonNull WebFilterChain chain) {
        String incomingCorrelationId = exchange.getRequest()
            .getHeaders()
            .getFirst(GatewayHeaderNames.CORRELATION_ID);

        String correlationId = StringUtils.hasText(incomingCorrelationId)
            ? incomingCorrelationId
            : UUID.randomUUID().toString();

        ServerHttpRequest request = exchange.getRequest()
            .mutate()
            .headers(httpHeaders -> httpHeaders.set(GatewayHeaderNames.CORRELATION_ID, correlationId))
            .build();

        exchange.getResponse()
            .getHeaders()
            .set(GatewayHeaderNames.CORRELATION_ID, correlationId);

        return chain.filter(exchange.mutate().request(request).build());
    }

}
