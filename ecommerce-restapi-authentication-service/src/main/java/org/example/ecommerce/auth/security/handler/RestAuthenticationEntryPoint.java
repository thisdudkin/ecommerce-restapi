package org.example.ecommerce.auth.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ecommerce.auth.exception.utils.ProblemDetailsFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.AUTHENTICATION_REQUIRED_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.AUTHENTICATION_REQUIRED_TITLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(@NonNull HttpServletRequest request,
                         @NonNull HttpServletResponse response,
                         @NonNull AuthenticationException authException) throws IOException {
        response.setStatus(UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        objectMapper.writeValue(
            response.getOutputStream(),
            ProblemDetailsFactory.build(
                UNAUTHORIZED,
                AUTHENTICATION_REQUIRED_TITLE,
                AUTHENTICATION_REQUIRED_DETAIL,
                request
            )
        );
    }

}
