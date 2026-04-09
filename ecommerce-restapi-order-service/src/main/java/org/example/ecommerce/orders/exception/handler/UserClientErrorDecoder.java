package org.example.ecommerce.orders.exception.handler;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.example.ecommerce.orders.exception.custom.feign.UserNotFoundException;
import org.example.ecommerce.orders.exception.custom.feign.UserServiceUnavailableException;
import org.springframework.http.HttpStatus;

import java.util.Objects;

public class UserClientErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.resolve(response.status());
        return switch (Objects.requireNonNull(status)) {
            case NOT_FOUND -> new UserNotFoundException("User not found");
            case BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT ->
                new UserServiceUnavailableException("User service is temporarily unavailable");
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

}
