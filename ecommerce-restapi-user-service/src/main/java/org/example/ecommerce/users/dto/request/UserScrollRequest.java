package org.example.ecommerce.users.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.io.Serializable;

public record UserScrollRequest(
    String name,
    String surname,
    @Min(1) @Max(50)
    Integer size,
    String direction,
    String cursor
) implements Serializable {

    public int resolvedSize() {
        return size == null ? 20 : size;
    }

    public String resolvedDirection() {
        return direction == null || direction.isBlank()
            ? "DESC"
            : direction;
    }

}
