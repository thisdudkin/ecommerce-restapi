package org.example.ecommerce.payments.infrastructure.internal.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.example.ecommerce.payments.domain.exception.OrderNotFoundException;
import org.example.ecommerce.payments.infrastructure.exception.OrderServiceUnavailableException;
import org.springframework.http.HttpStatus;

public class OrderClientErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.valueOf(response.status());
        return switch (status) {
            case NOT_FOUND -> new OrderNotFoundException();
            case BAD_GATEWAY,
                 SERVICE_UNAVAILABLE,
                 GATEWAY_TIMEOUT -> new OrderServiceUnavailableException(
                     "Order service is temporarily unavailable. methodKey=%s, status=%s".formatted(methodKey, response.status())
            );
            default -> {
                Exception defaultException = defaultDecoder.decode(methodKey, response);
                yield new OrderServiceUnavailableException(
                    "Failed to get order from order service. methodKey=%s, status=%s"
                        .formatted(methodKey, response.status()),
                    defaultException
                );
            }
        };
    }

}
