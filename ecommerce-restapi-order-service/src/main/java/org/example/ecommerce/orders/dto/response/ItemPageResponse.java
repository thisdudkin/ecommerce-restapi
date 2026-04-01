package org.example.ecommerce.orders.dto.response;

import java.util.Collections;
import java.util.List;

public record ItemPageResponse(
    List<ItemResponse> items,
    String token
) {
    public static ItemPageResponse empty() {
        return new ItemPageResponse(Collections.emptyList(), null);
    }
}
