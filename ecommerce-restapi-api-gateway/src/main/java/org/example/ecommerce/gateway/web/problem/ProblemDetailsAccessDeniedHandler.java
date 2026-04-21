package org.example.ecommerce.gateway.web.problem;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ProblemDetailsAccessDeniedHandler implements ServerAccessDeniedHandler {

    private final ProblemDetailResponseWriter writer;

    public ProblemDetailsAccessDeniedHandler(ProblemDetailResponseWriter writer) {
        this.writer = writer;
    }

    @NonNull
    @Override
    public Mono<Void> handle(@NonNull ServerWebExchange exchange,
                             @NonNull AccessDeniedException denied) {
        return writer.write(
            exchange,
            HttpStatus.FORBIDDEN,
            "Forbidden",
            denied.getMessage()
        );
    }

}
