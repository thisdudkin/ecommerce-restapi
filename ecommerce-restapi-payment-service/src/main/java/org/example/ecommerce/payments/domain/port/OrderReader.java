package org.example.ecommerce.payments.domain.port;

import org.example.ecommerce.payments.domain.model.Order;

public interface OrderReader {
    Order getById(Long orderId);
}
