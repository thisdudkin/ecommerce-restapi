package org.example.ecommerce.users.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.ecommerce.users.exception.custom.InvalidCursorException;
import org.example.ecommerce.users.exception.custom.PaymentCardAlreadyActiveException;
import org.example.ecommerce.users.exception.custom.PaymentCardAlreadyInactiveException;
import org.example.ecommerce.users.exception.custom.PaymentCardNotFoundException;
import org.example.ecommerce.users.exception.custom.PaymentCardNumberAlreadyExistsException;
import org.example.ecommerce.users.exception.custom.PaymentCardOwnershipException;
import org.example.ecommerce.users.exception.custom.UserAlreadyActiveException;
import org.example.ecommerce.users.exception.custom.UserAlreadyInactiveException;
import org.example.ecommerce.users.exception.custom.UserEmailAlreadyExistsException;
import org.example.ecommerce.users.exception.custom.UserNotFoundException;
import org.example.ecommerce.users.exception.custom.UserPaymentCardsLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private static final String VALIDATION_FAILED_TITLE = "Validation failed";
    private static final String VALIDATION_FAILED_CODE = "VALIDATION_FAILED";

    private static final String MALFORMED_REQUEST_BODY_TITLE = "Malformed request body";
    private static final String MALFORMED_REQUEST_BODY_DETAIL = "Request body is missing or has invalid JSON format";
    private static final String MALFORMED_REQUEST_BODY_CODE = "MALFORMED_REQUEST_BODY";

    private static final String USER_NOT_FOUND_TITLE = "User not found";
    private static final String USER_NOT_FOUND_CODE = "USER_NOT_FOUND";

    private static final String PAYMENT_CARD_NOT_FOUND_TITLE = "Payment card not found";
    private static final String PAYMENT_CARD_NOT_FOUND_CODE = "PAYMENT_CARD_NOT_FOUND";

    private static final String USER_EMAIL_ALREADY_EXISTS_TITLE = "User email already exists";
    private static final String USER_EMAIL_ALREADY_EXISTS_CODE = "USER_EMAIL_ALREADY_EXISTS";

    private static final String PAYMENT_CARD_ALREADY_EXISTS_TITLE = "Payment card number already exists";
    private static final String PAYMENT_CARD_ALREADY_EXISTS_CODE = "PAYMENT_CARD_NUMBER_ALREADY_EXISTS";

    private static final String CONFLICT_TITLE = "Conflict";
    private static final String CONFLICT_DETAIL = "Request conflicts with current data state";
    private static final String DATA_INTEGRITY_VIOLATION_CODE = "DATA_INTEGRITY_VIOLATION";

    private static final String USER_PAYMENT_CARDS_LIMIT_EXCEEDED_TITLE = "User payment cards limit exceeded";
    private static final String USER_PAYMENT_CARDS_LIMIT_EXCEEDED_CODE = "USER_PAYMENT_CARDS_LIMIT_EXCEEDED";

    private static final String PAYMENT_CARD_OWNERSHIP_TITLE = "Payment card does not belong to user";
    private static final String PAYMENT_CARD_OWNERSHIP_CODE = "PAYMENT_CARD_OWNERSHIP_VIOLATION";

    private static final String USER_ALREADY_ACTIVE_TITLE = "User already active";
    private static final String USER_ALREADY_ACTIVE_CODE = "USER_ALREADY_ACTIVE";

    private static final String USER_ALREADY_INACTIVE_TITLE = "User already inactive";
    private static final String USER_ALREADY_INACTIVE_CODE = "USER_ALREADY_INACTIVE";

    private static final String PAYMENT_CARD_ALREADY_ACTIVE_TITLE = "Payment card already active";
    private static final String PAYMENT_CARD_ALREADY_ACTIVE_CODE = "PAYMENT_CARD_ALREADY_ACTIVE";

    private static final String PAYMENT_CARD_ALREADY_INACTIVE_TITLE = "Payment card already inactive";
    private static final String PAYMENT_CARD_ALREADY_INACTIVE_CODE = "PAYMENT_CARD_ALREADY_INACTIVE";

    private static final String INVALID_CURSOR_TITLE = "Invalid cursor";
    private static final String INVALID_CURSOR_CODE = "INVALID_CURSOR";

    private static final String ACCESS_DENIED_TITLE = "Access denied";
    private static final String ACCESS_DENIED_DETAIL = "You do not have permission to access this resource";
    private static final String ACCESS_DENIED_CODE = "ACCESS_DENIED";

    private static final String INTERNAL_SERVER_ERROR_TITLE = "Internal server error";
    private static final String INTERNAL_SERVER_ERROR_DETAIL = "An unexpected error occurred";
    private static final String INTERNAL_SERVER_ERROR_CODE = "INTERNAL_SERVER_ERROR";

    private static final String USERS_EMAIL_KEY_CONSTRAINT = "users_email_key";
    private static final String PAYMENTS_CARDS_NUMBER_KEY_CONSTRAINT = "payments_cards_number_key";

    private static final String NUMBERS_PROPERTY = "numbers";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                                  HttpServletRequest request) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        log.debug("Validation failed: {}", fieldErrors);

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, VALIDATION_FAILED_TITLE, buildValidationDetail(fieldErrors), VALIDATION_FAILED_CODE, request)
                .withErrors(toFieldViolations(fieldErrors))
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiProblemDetail> handleConstraintViolationException(ConstraintViolationException e,
                                                                               HttpServletRequest request) {
        String detail = e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));

        log.debug("Constraint violation: {}", detail);

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, VALIDATION_FAILED_TITLE, detail, VALIDATION_FAILED_CODE, request)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiProblemDetail> handleHandlerMethodValidationException(HandlerMethodValidationException e,
                                                                                   HttpServletRequest request) {
        String detail = e.getAllErrors().stream()
            .map(error -> error.getDefaultMessage() == null ? error.toString() : error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));

        log.debug("Handler method validation failed: {}", detail);

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, VALIDATION_FAILED_TITLE, detail, VALIDATION_FAILED_CODE, request)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException e,
                                                                                  HttpServletRequest request) {
        log.debug("Malformed request body: {}", e.getMessage());

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(
                BAD_REQUEST,
                MALFORMED_REQUEST_BODY_TITLE,
                MALFORMED_REQUEST_BODY_DETAIL,
                MALFORMED_REQUEST_BODY_CODE,
                request
            )
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiProblemDetail> handleUserNotFoundException(UserNotFoundException e,
                                                                        HttpServletRequest request) {
        log.debug("User not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            problem(NOT_FOUND, USER_NOT_FOUND_TITLE, e.getMessage(), USER_NOT_FOUND_CODE, request)
        );
    }

    @ExceptionHandler(PaymentCardNotFoundException.class)
    public ResponseEntity<ApiProblemDetail> handlePaymentCardNotFoundException(PaymentCardNotFoundException e,
                                                                               HttpServletRequest request) {
        log.debug("Payment card not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            problem(NOT_FOUND, PAYMENT_CARD_NOT_FOUND_TITLE, e.getMessage(), PAYMENT_CARD_NOT_FOUND_CODE, request)
        );
    }

    @ExceptionHandler(UserEmailAlreadyExistsException.class)
    public ResponseEntity<ApiProblemDetail> handleUserEmailAlreadyExistsException(UserEmailAlreadyExistsException e,
                                                                                  HttpServletRequest request) {
        log.debug("User email already exists: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, USER_EMAIL_ALREADY_EXISTS_TITLE, e.getMessage(), USER_EMAIL_ALREADY_EXISTS_CODE, request)
        );
    }

    @ExceptionHandler(PaymentCardNumberAlreadyExistsException.class)
    public ResponseEntity<ApiProblemDetail> handlePaymentCardNumberAlreadyExistsException(PaymentCardNumberAlreadyExistsException e,
                                                                                          HttpServletRequest request) {
        log.debug("Payment card number already exists: {}", e.getMessage());

        ApiProblemDetail problem = problem(
            CONFLICT,
            PAYMENT_CARD_ALREADY_EXISTS_TITLE,
            e.getMessage(),
            PAYMENT_CARD_ALREADY_EXISTS_CODE,
            request
        );
        problem.setProperty(NUMBERS_PROPERTY, e.getNumbers());

        return ResponseEntity.status(CONFLICT).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiProblemDetail> handleDataIntegrityViolationException(DataIntegrityViolationException e,
                                                                                  HttpServletRequest request) {
        String rootMessage = e.getMostSpecificCause().getMessage();

        String title = CONFLICT_TITLE;
        String detail = CONFLICT_DETAIL;
        String errorCode = DATA_INTEGRITY_VIOLATION_CODE;

        if (rootMessage != null) {
            if (rootMessage.contains(USERS_EMAIL_KEY_CONSTRAINT)) {
                title = USER_EMAIL_ALREADY_EXISTS_TITLE;
                detail = USER_EMAIL_ALREADY_EXISTS_TITLE;
                errorCode = USER_EMAIL_ALREADY_EXISTS_CODE;
            } else if (rootMessage.contains(PAYMENTS_CARDS_NUMBER_KEY_CONSTRAINT)) {
                title = PAYMENT_CARD_ALREADY_EXISTS_TITLE;
                detail = PAYMENT_CARD_ALREADY_EXISTS_TITLE;
                errorCode = PAYMENT_CARD_ALREADY_EXISTS_CODE;
            }
        }

        log.debug("Data integrity violation: {}", rootMessage);

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, title, detail, errorCode, request)
        );
    }

    @ExceptionHandler(UserPaymentCardsLimitExceededException.class)
    public ResponseEntity<ApiProblemDetail> handleUserPaymentCardsLimitExceededException(UserPaymentCardsLimitExceededException e,
                                                                                         HttpServletRequest request) {
        log.debug("User payment cards limit exceeded: {}", e.getMessage());

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(
                BAD_REQUEST,
                USER_PAYMENT_CARDS_LIMIT_EXCEEDED_TITLE,
                e.getMessage(),
                USER_PAYMENT_CARDS_LIMIT_EXCEEDED_CODE,
                request
            )
        );
    }

    @ExceptionHandler(PaymentCardOwnershipException.class)
    public ResponseEntity<ApiProblemDetail> handlePaymentCardOwnershipException(PaymentCardOwnershipException e,
                                                                                HttpServletRequest request) {
        log.debug("Payment card ownership violation: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(
                CONFLICT,
                PAYMENT_CARD_OWNERSHIP_TITLE,
                e.getMessage(),
                PAYMENT_CARD_OWNERSHIP_CODE,
                request
            )
        );
    }

    @ExceptionHandler(UserAlreadyActiveException.class)
    public ResponseEntity<ApiProblemDetail> handleUserAlreadyActiveException(UserAlreadyActiveException e,
                                                                             HttpServletRequest request) {
        log.debug("User already active: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, USER_ALREADY_ACTIVE_TITLE, e.getMessage(), USER_ALREADY_ACTIVE_CODE, request)
        );
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ResponseEntity<ApiProblemDetail> handleUserAlreadyInactiveException(UserAlreadyInactiveException e,
                                                                               HttpServletRequest request) {
        log.debug("User already inactive: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, USER_ALREADY_INACTIVE_TITLE, e.getMessage(), USER_ALREADY_INACTIVE_CODE, request)
        );
    }

    @ExceptionHandler(PaymentCardAlreadyActiveException.class)
    public ResponseEntity<ApiProblemDetail> handlePaymentCardAlreadyActiveException(PaymentCardAlreadyActiveException e,
                                                                                    HttpServletRequest request) {
        log.debug("Payment card already active: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, PAYMENT_CARD_ALREADY_ACTIVE_TITLE, e.getMessage(), PAYMENT_CARD_ALREADY_ACTIVE_CODE, request)
        );
    }

    @ExceptionHandler(PaymentCardAlreadyInactiveException.class)
    public ResponseEntity<ApiProblemDetail> handlePaymentCardAlreadyInactiveException(PaymentCardAlreadyInactiveException e,
                                                                                      HttpServletRequest request) {
        log.debug("Payment card already inactive: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, PAYMENT_CARD_ALREADY_INACTIVE_TITLE, e.getMessage(), PAYMENT_CARD_ALREADY_INACTIVE_CODE, request)
        );
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ApiProblemDetail> handleInvalidCursorException(InvalidCursorException e,
                                                                         HttpServletRequest request) {
        log.debug("Invalid cursor: {}", e.getMessage());

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, INVALID_CURSOR_TITLE, e.getMessage(), INVALID_CURSOR_CODE, request)
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiProblemDetail> handleAccessDeniedException(AccessDeniedException e,
                                                                        HttpServletRequest request) {
        log.debug("Access denied: {}", e.getMessage());

        return ResponseEntity.status(FORBIDDEN).body(
            problem(
                FORBIDDEN,
                ACCESS_DENIED_TITLE,
                ACCESS_DENIED_DETAIL,
                ACCESS_DENIED_CODE,
                request
            )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblemDetail> handleException(Exception e,
                                                            HttpServletRequest request) {
        log.error("Unhandled exception", e);

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(
            problem(
                INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_TITLE,
                INTERNAL_SERVER_ERROR_DETAIL,
                INTERNAL_SERVER_ERROR_CODE,
                request
            )
        );
    }

    private ApiProblemDetail problem(HttpStatus status,
                                     String title,
                                     String detail,
                                     String errorCode,
                                     HttpServletRequest request) {
        return ApiProblemDetail.of(status, title, detail, request.getRequestURI())
            .withErrorCode(errorCode);
    }

    private String buildValidationDetail(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
    }

    private List<ApiProblemDetail.FieldViolation> toFieldViolations(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
            .map(error -> new ApiProblemDetail.FieldViolation(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .toList();
    }
}
