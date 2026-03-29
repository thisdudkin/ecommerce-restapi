package org.example.ecommerce.auth.exception.handler;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.example.ecommerce.auth.exception.custom.DownstreamServiceUnavailableException;
import org.example.ecommerce.auth.exception.custom.UserAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UserClientErrorDecoderTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private UserClientErrorDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new UserClientErrorDecoder(objectMapper);
    }

    @Test
    void decodeShouldReturnUserAlreadyExistsExceptionWithProblemDetailMessage() throws Exception {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "User with email already exists"
        );

        Exception actual = decoder.decode(
            "UserClient#create",
            response(HttpStatus.CONFLICT.value(), objectMapper.writeValueAsString(body))
        );

        assertInstanceOf(UserAlreadyExistsException.class, actual);
        assertEquals("User with email already exists", actual.getMessage());
    }

    @Test
    void decodeShouldReturnDefaultDetailWhenBodyIsNull() {
        Exception actual = decoder.decode(
            "UserClient#create",
            response(HttpStatus.CONFLICT.value(), null)
        );

        assertInstanceOf(UserAlreadyExistsException.class, actual);
        assertEquals("User already exists", actual.getMessage());
    }

    @Test
    void decodeShouldReturnDefaultDetailWhenBodyIsBlank() {
        Exception actual = decoder.decode(
            "UserClient#create",
            response(HttpStatus.CONFLICT.value(), "   ")
        );

        assertInstanceOf(UserAlreadyExistsException.class, actual);
        assertEquals("User already exists", actual.getMessage());
    }

    @Test
    void decodeShouldReturnServiceUnavailableExceptionFor502() {
        Exception actual = decoder.decode(
            "UserClient#create",
            response(HttpStatus.BAD_GATEWAY.value(), null)
        );

        assertInstanceOf(DownstreamServiceUnavailableException.class, actual);
        assertEquals("User service is temporarily unavailable", actual.getMessage());
    }

    @Test
    void decodeShouldReturnServiceUnavailableExceptionFor503() {
        Exception actual = decoder.decode(
            "UserClient#create",
            response(HttpStatus.SERVICE_UNAVAILABLE.value(), null)
        );

        assertInstanceOf(DownstreamServiceUnavailableException.class, actual);
        assertEquals("User service is temporarily unavailable", actual.getMessage());
    }

    @Test
    void decodeShouldReturnServiceUnavailableExceptionFor504() {
        Exception actual = decoder.decode(
            "UserClient#create",
            response(HttpStatus.GATEWAY_TIMEOUT.value(), null)
        );

        assertInstanceOf(DownstreamServiceUnavailableException.class, actual);
        assertEquals("User service is temporarily unavailable", actual.getMessage());
    }

    @Test
    void decodeShouldDelegateToDefaultDecoderForUnexpectedStatus() {
        Exception actual = decoder.decode(
            "UserClient#create",
            response(HttpStatus.INTERNAL_SERVER_ERROR.value(), null)
        );

        assertInstanceOf(FeignException.class, actual);
    }

    private Response response(int status, String body) {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "http://localhost/api/v1/users",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            new RequestTemplate()
        );

        Response.Builder builder = Response.builder()
            .status(status)
            .reason("reason")
            .request(request)
            .headers(Map.of());

        if (body != null) {
            builder.body(body, StandardCharsets.UTF_8);
        }

        return builder.build();
    }
}
