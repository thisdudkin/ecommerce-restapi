package org.example.ecommerce.auth.exception.handler;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.example.ecommerce.auth.exception.custom.DownstreamServiceUnavailableException;
import org.example.ecommerce.auth.exception.custom.UserAlreadyExistsException;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class UserClientErrorDecoder implements ErrorDecoder {

    private static final String USER_ALREADY_EXISTS = "User already exists";

    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper;

    public UserClientErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            case 409 -> new UserAlreadyExistsException(
                USER_ALREADY_EXISTS,
                readDetail(response)
            );
            case 502, 503, 504 -> new DownstreamServiceUnavailableException(
                "User service is temporarily unavailable"
            );
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

    private String readDetail(Response response) {
        if (response.body() == null) {
            return USER_ALREADY_EXISTS;
        }

        try {
            String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));

            if (body.isBlank()) {
                return USER_ALREADY_EXISTS;
            }

            ProblemDetail problemDetail = objectMapper.readValue(body, ProblemDetail.class);
            String detail = problemDetail.getDetail();

            return (detail == null || detail.isBlank()) ? USER_ALREADY_EXISTS : detail;

        } catch (IOException e) {
            return USER_ALREADY_EXISTS;
        }
    }

}
