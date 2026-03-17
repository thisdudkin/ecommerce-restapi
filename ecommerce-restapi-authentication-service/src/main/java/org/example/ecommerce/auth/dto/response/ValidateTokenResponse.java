package org.example.ecommerce.auth.dto.response;

import org.example.ecommerce.auth.security.enums.JwtType;
import org.example.ecommerce.auth.security.enums.Role;

import java.io.Serializable;

public record ValidateTokenResponse(
    boolean valid,
    Long userId,
    Role role,
    JwtType tokenType
) implements Serializable {
    public static ValidateTokenResponse invalid() {
        return new ValidateTokenResponse(false, null, null, null);
    }
}
