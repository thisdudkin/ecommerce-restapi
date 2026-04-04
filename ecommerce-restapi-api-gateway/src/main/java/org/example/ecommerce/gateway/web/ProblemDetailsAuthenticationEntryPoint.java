package org.example.ecommerce.gateway.web;

import org.example.ecommerce.gateway.exception.TokenValidationServiceUnavailableException;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ProblemDetailsAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {

    private final ProblemDetailResponseWriter writer;

    public ProblemDetailsAuthenticationEntryPoint(ProblemDetailResponseWriter writer) {
        this.writer = writer;
    }

    @NonNull
    @Override
    public Mono<Void> commence(@NonNull ServerWebExchange exchange,
                               @NonNull AuthenticationException ex) {
        HttpStatus status = ex instanceof TokenValidationServiceUnavailableException
            ? HttpStatus.SERVICE_UNAVAILABLE
            : HttpStatus.UNAUTHORIZED;

        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            status,
            ex.getMessage() != null
                ? ex.getMessage()
                : (status == HttpStatus.SERVICE_UNAVAILABLE
                   ? "Authentication service is unavailable"
                   : "Authentication is required")
        );

        detail.setTitle(status == HttpStatus.SERVICE_UNAVAILABLE
            ? "Authentication service unavailable"
            : "Unauthorized");

        return writer.write(
            exchange.getResponse(),
            detail,
            exchange.getRequest().getPath().value()
        );
    }

}
