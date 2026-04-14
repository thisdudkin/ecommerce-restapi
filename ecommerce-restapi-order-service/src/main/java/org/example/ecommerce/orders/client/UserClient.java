package org.example.ecommerce.orders.client;

import org.example.ecommerce.orders.config.UserClientFeignConfig;
import org.example.ecommerce.orders.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(
    name = "user-service",
    url = "${clients.user-service.base-url}",
    configuration = UserClientFeignConfig.class,
    fallbackFactory = UserClientFallbackFactory.class
)
public interface UserClient {

    @GetMapping("/api/v1/users/{userId}")
    UserResponse getById(@PathVariable Long userId);

    @GetMapping("/api/v1/users")
    List<UserResponse> getByIds(@RequestParam(value = "id") Collection<Long> ids);

}
