package org.example.ecommerce.auth.exception.utils;

public final class ProblemDetailsConstants {

    private ProblemDetailsConstants() {
    }

    public static final String VALIDATION_FAILED_TITLE = "Validation failed";

    public static final String MALFORMED_REQUEST_BODY_TITLE = "Malformed request body";
    public static final String MALFORMED_REQUEST_BODY_DETAIL = "Request body is missing or has invalid JSON format";

    public static final String CREDENTIAL_ALREADY_EXISTS_TITLE = "Credential already exists";
    public static final String CREDENTIAL_NOT_FOUND_TITLE = "Credential not found";

    public static final String AUTHENTICATION_FAILED_TITLE = "Authentication failed";
    public static final String AUTHENTICATION_FAILED_DETAIL = "Invalid login or password";

    public static final String CREDENTIAL_INACTIVE_TITLE = "Credential inactive";
    public static final String CREDENTIAL_INACTIVE_DETAIL = "Credential is inactive";

    public static final String INVALID_TOKEN_TITLE = "Invalid token";

    public static final String SERVICE_UNAVAILABLE_TITLE = "Service unavailable";
    public static final String SERVICE_UNAVAILABLE_DETAIL = "Dependent service is temporarily unavailable";

    public static final String REGISTRATION_PARTIALLY_FAILED_TITLE = "Registration partially failed";
    public static final String REGISTRATION_PARTIALLY_FAILED_DETAIL =
        "User was created in user-service, but cleanup could not be completed immediately. The incident was saved for retry.";

    public static final String ACCESS_DENIED_TITLE = "Access denied";
    public static final String ACCESS_DENIED_DETAIL = "You do not have permission to access this resource";

    public static final String INTERNAL_SERVER_ERROR_TITLE = "Internal server error";
    public static final String INTERNAL_SERVER_ERROR_DETAIL = "An unexpected error occurred";

    public static final String AUTHENTICATION_REQUIRED_TITLE = "Authentication required";
    public static final String AUTHENTICATION_REQUIRED_DETAIL = "Authentication is required to access this resource";

}
