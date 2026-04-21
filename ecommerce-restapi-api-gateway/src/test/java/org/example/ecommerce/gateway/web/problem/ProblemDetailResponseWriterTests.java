package org.example.ecommerce.gateway.web.problem;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProblemDetailResponseWriterTests {

    @Test
    void writeShouldPopulateProblemJsonResponse() {
        ProblemDetailResponseWriter writer = new ProblemDetailResponseWriter(new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/test").build()
        );

        StepVerifier.create(writer.write(exchange, HttpStatus.BAD_REQUEST, "Bad request", "Validation failed"))
            .verifyComplete();

        String body = exchange.getResponse().getBodyAsString().block();

        assertThat(body).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(exchange.getResponse().getHeaders().getContentLength())
            .isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
        assertThat(body).contains("\"title\":\"Bad request\"");
        assertThat(body).contains("\"status\":400");
        assertThat(body).contains("\"detail\":\"Validation failed\"");
    }

    @Test
    void writeShouldUseFallbackBodyWhenSerializationFails() {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("boom"));

        ProblemDetailResponseWriter writer = new ProblemDetailResponseWriter(objectMapper);
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/test").build()
        );

        StepVerifier.create(writer.write(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required"))
            .verifyComplete();

        String body = exchange.getResponse().getBodyAsString().block();

        assertThat(body).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(exchange.getResponse().getHeaders().getContentLength())
            .isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
        assertThat(body).contains("\"title\": \"Internal Server Error\"");
        assertThat(body).contains("\"status\": 500");
        assertThat(body).contains("\"detail\": \"Unable to serialize problem detail\"");
    }

}
