package org.example.ecommerce.gateway.web.problem;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
public class ProblemDetailResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemDetailResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerWebExchange exchange,
                            HttpStatus status,
                            String title,
                            String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);

        byte[] responseBody = serialize(problemDetail);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        exchange.getResponse().getHeaders().setContentLength(responseBody.length);

        return exchange.getResponse()
            .writeWith(Mono.just(exchange.getResponse()
                .bufferFactory()
                .wrap(responseBody)));
    }

    private byte[] serialize(ProblemDetail problemDetail) {
        try {
            return objectMapper.writeValueAsString(problemDetail).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            String fallback = """
                {
                    "title": "Internal Server Error",
                    "status": 500,
                    "detail": "Unable to serialize problem detail"
                }
                """;
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

}
