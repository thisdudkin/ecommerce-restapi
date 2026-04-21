package org.example.ecommerce.gateway.web.problem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.access.AccessDeniedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemDetailsAccessDeniedHandlerTests {

    @Mock
    private ProblemDetailResponseWriter writer;

    @InjectMocks
    private ProblemDetailsAccessDeniedHandler handler;

    @Test
    void handleShouldDelegateForbiddenProblemToWriter() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/orders").build()
        );
        AccessDeniedException exception = new AccessDeniedException("Access is denied");
        Mono<Void> result = Mono.empty();

        when(writer.write(exchange, HttpStatus.FORBIDDEN, "Forbidden", "Access is denied"))
            .thenReturn(result);

        StepVerifier.create(handler.handle(exchange, exception))
            .verifyComplete();

        verify(writer).write(exchange, HttpStatus.FORBIDDEN, "Forbidden", "Access is denied");
        verifyNoMoreInteractions(writer);
    }

}
