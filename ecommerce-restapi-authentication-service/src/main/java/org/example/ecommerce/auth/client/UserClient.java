package org.example.ecommerce.auth.client;

import org.example.ecommerce.auth.config.UserClientFeignConfig;
import org.example.ecommerce.auth.dto.request.UserRequest;
import org.example.ecommerce.auth.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "user-service",
    url = "${clients.user-service.base-url}",
    configuration = UserClientFeignConfig.class
)
public interface UserClient {

    @PostMapping("/api/v1/users")
    UserResponse create(@RequestBody UserRequest request);

    @DeleteMapping("/api/v1/users/{userId}")
    void delete(@PathVariable Long userId);

}
