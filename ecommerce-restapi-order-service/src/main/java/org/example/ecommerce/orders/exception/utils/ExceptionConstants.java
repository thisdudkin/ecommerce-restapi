package org.example.ecommerce.orders.exception.utils;

public final class ExceptionConstants {

    private ExceptionConstants() {
    }

    public static final String TITLE_VALIDATION_FAILED = "Validation failed";
    public static final String TITLE_MALFORMED_REQUEST_BODY = "Malformed request body";
    public static final String TITLE_BAD_REQUEST = "Bad request";
    public static final String TITLE_ITEM_NOT_FOUND = "Item not found";
    public static final String TITLE_ORDER_NOT_FOUND = "Order not found";
    public static final String TITLE_ORDER_ITEM_NOT_FOUND = "Order item not found";
    public static final String TITLE_EMPTY_ORDER = "Empty order";
    public static final String TITLE_ORDER_STATE_CONFLICT = "Order state conflict";
    public static final String TITLE_INVALID_CURSOR = "Invalid cursor";
    public static final String TITLE_USER_NOT_FOUND = "User not found";
    public static final String TITLE_USER_SERVICE_UNAVAILABLE = "User service unavailable";
    public static final String TITLE_ACCESS_DENIED = "Access denied";
    public static final String TITLE_INVALID_TOKEN = "Invalid token";
    public static final String TITLE_INTERNAL_SERVER_ERROR = "Internal server error";

    public static final String DETAIL_MALFORMED_REQUEST_BODY = "Request body is missing or has invalid JSON format";
    public static final String DETAIL_ACCESS_DENIED = "You do not have permission to access this resource";
    public static final String DETAIL_INVALID_TOKEN = "JWT token is invalid or expired";
    public static final String DETAIL_INTERNAL_SERVER_ERROR = "An unexpected error occurred";

}
