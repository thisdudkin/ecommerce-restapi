package org.example.ecommerce.payments.infrastructure.internal.adapter;

import lombok.RequiredArgsConstructor;
import org.example.ecommerce.payments.domain.model.Order;
import org.example.ecommerce.payments.domain.port.OrderReader;
import org.example.ecommerce.payments.infrastructure.internal.client.OrderClient;
import org.example.ecommerce.payments.infrastructure.internal.dto.OrderResponse;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeignOrderReader implements OrderReader {

    private final OrderClient client;

    @Override
    public Order getById(Long orderId) {
        OrderResponse response = client.getById(orderId);

        return new Order(
            response.id(),
            response.user() != null
                ? response.user().id()
                : null,
            response.status(),
            response.totalPrice(),
            response.deleted()
        );
    }

}
