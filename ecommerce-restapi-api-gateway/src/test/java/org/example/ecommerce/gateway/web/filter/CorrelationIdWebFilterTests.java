package org.example.ecommerce.gateway.web.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrelationIdWebFilterTests {

    private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

    @Test
    void filterShouldReuseIncomingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users")
                .header(CorrelationIdWebFilter.CORRELATION_ID_HEADER, "corr-123")
                .build()
        );

        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        WebFilterChain chain = webExchange -> {
            forwardedExchange.set(webExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertNotNull(forwardedExchange.get());
        assertEquals(
            "corr-123",
            forwardedExchange.get().getRequest().getHeaders().getFirst(CorrelationIdWebFilter.CORRELATION_ID_HEADER)
        );
        assertEquals(
            "corr-123",
            exchange.getResponse().getHeaders().getFirst(CorrelationIdWebFilter.CORRELATION_ID_HEADER)
        );
    }

    @Test
    void filterShouldGenerateCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/users").build()
        );

        AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
        WebFilterChain chain = webExchange -> {
            forwardedExchange.set(webExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        String requestCorrelationId = forwardedExchange.get()
            .getRequest()
            .getHeaders()
            .getFirst(CorrelationIdWebFilter.CORRELATION_ID_HEADER);

        String responseCorrelationId = exchange.getResponse()
            .getHeaders()
            .getFirst(CorrelationIdWebFilter.CORRELATION_ID_HEADER);

        assertNotNull(requestCorrelationId);
        assertFalse(requestCorrelationId.isBlank());
        assertEquals(requestCorrelationId, responseCorrelationId);
    }

}
