package org.example.ecommerce.gateway.domain.auth.model;

public record AuthenticatedUser(
    Long userId,
    String role,
    String tokenType
) {

    public static AuthenticatedUser from(AuthenticationValidationResult result) {
        return new AuthenticatedUser(
            result.userId(),
            result.role(),
            result.tokenType()
        );
    }

}
