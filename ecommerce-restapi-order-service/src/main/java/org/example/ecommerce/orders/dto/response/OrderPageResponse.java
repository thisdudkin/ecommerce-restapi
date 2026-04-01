package org.example.ecommerce.orders.dto.response;

import java.util.Collections;
import java.util.List;

public record OrderPageResponse(
    List<OrderResponse> orders,
    String token
) {
    public static OrderPageResponse empty() {
        return new OrderPageResponse(Collections.emptyList(), null);
    }
}
