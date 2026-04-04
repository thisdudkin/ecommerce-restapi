package org.example.ecommerce.gateway.web;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
                             @NonNull AccessDeniedException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage() != null
                ? ex.getMessage()
                : "Access is denied"
        );

        return writer.write(
            exchange.getResponse(),
            detail,
            exchange.getRequest().getPath().value()
        );
    }

}
