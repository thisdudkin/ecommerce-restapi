package org.example.ecommerce.orders.dto.request;

import jakarta.validation.constraints.NotNull;
import org.example.ecommerce.orders.enums.OrderStatus;

public record OrderStatusUpdateRequest(
    @NotNull OrderStatus status
) { }
