package org.example.ecommerce.gateway.web.problem;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
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
        return writer.write(
            exchange,
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            ex.getMessage()
        );
    }

}
