package org.example.ecommerce.orders.client;

import feign.FeignException;
import org.example.ecommerce.orders.exception.custom.feign.UserNotFoundException;
import org.example.ecommerce.orders.exception.custom.feign.UserServiceUnavailableException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return userId -> {
            if (cause instanceof UserNotFoundException e)
                throw e;
            if (cause instanceof UserServiceUnavailableException e)
                throw e;
            if (cause instanceof FeignException.NotFound)
                throw new UserNotFoundException("User not found");

            throw new UserServiceUnavailableException("User service is temporarily unavailable");
        };
    }

}
