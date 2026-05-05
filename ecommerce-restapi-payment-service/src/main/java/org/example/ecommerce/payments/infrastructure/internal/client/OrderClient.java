package org.example.ecommerce.payments.infrastructure.internal.client;

import org.example.ecommerce.payments.infrastructure.internal.config.OrderClientConfiguration;
import org.example.ecommerce.payments.infrastructure.internal.dto.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "order-service",
    url = "${clients.order-service.base-url}",
    configuration = OrderClientConfiguration.class
)
public interface OrderClient {

    @GetMapping("/api/v1/orders/{orderId}")
    OrderResponse getById(@PathVariable Long orderId);

}
