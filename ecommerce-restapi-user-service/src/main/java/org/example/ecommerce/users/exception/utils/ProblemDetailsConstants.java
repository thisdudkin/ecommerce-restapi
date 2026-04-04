package org.example.ecommerce.users.exception.utils;

public final class ProblemDetailsConstants {

    private ProblemDetailsConstants() {
    }

    public static final String VALIDATION_FAILED_TITLE = "Validation failed";

    public static final String MALFORMED_REQUEST_BODY_TITLE = "Malformed request body";
    public static final String MALFORMED_REQUEST_BODY_DETAIL = "Request body is missing or has invalid JSON format";

    public static final String USER_NOT_FOUND_TITLE = "User not found";
    public static final String PAYMENT_CARD_NOT_FOUND_TITLE = "Payment card not found";

    public static final String USER_EMAIL_ALREADY_EXISTS_TITLE = "User email already exists";
    public static final String PAYMENT_CARD_ALREADY_EXISTS_TITLE = "Payment card number already exists";

    public static final String CONFLICT_TITLE = "Conflict";
    public static final String CONFLICT_DETAIL = "Request conflicts with current data state";

    public static final String USER_PAYMENT_CARDS_LIMIT_EXCEEDED_TITLE = "User payment cards limit exceeded";
    public static final String PAYMENT_CARD_OWNERSHIP_TITLE = "Payment card does not belong to user";

    public static final String USER_ALREADY_ACTIVE_TITLE = "User already active";
    public static final String USER_ALREADY_INACTIVE_TITLE = "User already inactive";

    public static final String PAYMENT_CARD_ALREADY_ACTIVE_TITLE = "Payment card already active";
    public static final String PAYMENT_CARD_ALREADY_INACTIVE_TITLE = "Payment card already inactive";

    public static final String INVALID_CURSOR_TITLE = "Invalid cursor";

    public static final String ACCESS_DENIED_TITLE = "Access denied";
    public static final String ACCESS_DENIED_DETAIL = "You do not have permission to access this resource";

    public static final String INVALID_TOKEN_TITLE = "Invalid token";
    public static final String INVALID_TOKEN_DETAIL = "JWT token is invalid or expired";

    public static final String INTERNAL_SERVER_ERROR_TITLE = "Internal server error";
    public static final String INTERNAL_SERVER_ERROR_DETAIL = "An unexpected error occurred";

}
