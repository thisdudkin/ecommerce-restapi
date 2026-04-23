package org.example.ecommerce.orders.exception.handler;

import feign.FeignException;
import org.example.ecommerce.orders.client.UserClient;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.example.ecommerce.orders.exception.custom.feign.UserNotFoundException;
import org.example.ecommerce.orders.exception.custom.feign.UserServiceUnavailableException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return new UserClient() {

            @Override
            public UserResponse getById(Long userId) {
                throw mapException(cause);
            }

            @Override
            public List<UserResponse> getByIds(Collection<Long> ids) {
                throw mapException(cause);
            }

        };
    }

    private RuntimeException mapException(Throwable cause) {
        if (cause instanceof UserNotFoundException e)
            return e;

        if (cause instanceof UserServiceUnavailableException e)
            return e;

        if (cause instanceof FeignException.NotFound)
            return new UserNotFoundException("User not found");

        return new UserServiceUnavailableException("User service is temporarily unavailable");
    }

}
