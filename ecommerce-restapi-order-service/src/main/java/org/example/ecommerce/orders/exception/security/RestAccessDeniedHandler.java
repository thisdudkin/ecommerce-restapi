package org.example.ecommerce.orders.exception.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ecommerce.orders.exception.utils.ProblemDetailsFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.DETAIL_ACCESS_DENIED;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_ACCESS_DENIED;

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
            ProblemDetailsFactory.build(
                HttpStatus.FORBIDDEN,
                TITLE_ACCESS_DENIED,
                DETAIL_ACCESS_DENIED,
                request
            )
        );
    }

}
