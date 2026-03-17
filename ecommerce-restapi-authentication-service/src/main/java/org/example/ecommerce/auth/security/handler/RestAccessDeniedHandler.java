package org.example.ecommerce.auth.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ecommerce.auth.exception.handler.ApiProblemDetail;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
            response.getOutputStream(),
            ApiProblemDetail.of(
                    HttpStatus.FORBIDDEN,
                    "Access denied",
                    "You do not have permission to access this resource",
                    request.getRequestURI()
                )
                .withErrorCode("ACCESS_DENIED")
        );
    }

}
