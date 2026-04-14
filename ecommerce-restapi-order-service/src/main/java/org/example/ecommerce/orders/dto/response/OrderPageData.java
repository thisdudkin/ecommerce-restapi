package org.example.ecommerce.orders.dto.response;

import org.example.ecommerce.orders.entity.Order;

import java.util.List;

public record OrderPageData(
    List<Order> orders,
    String token
) {
    public static OrderPageData empty() {
        return new OrderPageData(List.of(), null);
    }
}
