package org.example.ecommerce.auth.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ecommerce.auth.exception.utils.ProblemDetailsFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.ACCESS_DENIED_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.ACCESS_DENIED_TITLE;
import static org.springframework.http.HttpStatus.FORBIDDEN;

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
        response.setStatus(FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
            response.getOutputStream(),
            ProblemDetailsFactory.build(
                FORBIDDEN,
                ACCESS_DENIED_TITLE,
                ACCESS_DENIED_DETAIL,
                request
            )
        );
    }

}
