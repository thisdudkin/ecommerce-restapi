package org.example.ecommerce.orders.exception.handler;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.example.ecommerce.orders.exception.custom.feign.UserNotFoundException;
import org.example.ecommerce.orders.exception.custom.feign.UserServiceUnavailableException;

public class UserClientErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        return switch (response.status()) {
            // TODO
            case 404 -> new UserNotFoundException("User not found");
            case 502, 503, 504 -> new UserServiceUnavailableException("User service is temporarily unavailable");
            default -> defaultDecoder.decode(methodKey, response);
        };
    }

}
