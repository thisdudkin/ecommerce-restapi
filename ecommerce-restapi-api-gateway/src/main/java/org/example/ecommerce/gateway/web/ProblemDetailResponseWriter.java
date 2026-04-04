package org.example.ecommerce.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Component
public class ProblemDetailResponseWriter {

    private final ObjectMapper objectMapper;

    public ProblemDetailResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Mono<Void> write(ServerHttpResponse response,
                            ProblemDetail problemDetail,
                            String path) {
        response.setStatusCode(HttpStatus.valueOf(problemDetail.getStatus()));
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        problemDetail.setProperty("path", path);
        problemDetail.setProperty("timestamp", LocalDateTime.now());

        try {
            byte[] body = objectMapper.writeValueAsBytes(problemDetail);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (Exception ex) {
            byte[] fallback = """
                {
                    "title": "Internal Server Error",
                    "status": 500,
                    "detail": "Unable to serialize ProblemDetail"
                }
                """.getBytes();
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }

    }

}
