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
import org.example.ecommerce.users.exception.utils.ProblemDetailsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.ACCESS_DENIED_DETAIL;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.ACCESS_DENIED_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.CONFLICT_DETAIL;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.CONFLICT_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.INTERNAL_SERVER_ERROR_DETAIL;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.INTERNAL_SERVER_ERROR_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.INVALID_CURSOR_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.MALFORMED_REQUEST_BODY_DETAIL;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.MALFORMED_REQUEST_BODY_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.PAYMENT_CARD_ALREADY_ACTIVE_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.PAYMENT_CARD_ALREADY_EXISTS_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.PAYMENT_CARD_ALREADY_INACTIVE_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.PAYMENT_CARD_NOT_FOUND_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.PAYMENT_CARD_OWNERSHIP_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.USER_ALREADY_ACTIVE_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.USER_ALREADY_INACTIVE_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.USER_EMAIL_ALREADY_EXISTS_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.USER_NOT_FOUND_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.USER_PAYMENT_CARDS_LIMIT_EXCEEDED_TITLE;
import static org.example.ecommerce.users.exception.utils.ProblemDetailsConstants.VALIDATION_FAILED_TITLE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private static final String USERS_EMAIL_KEY_CONSTRAINT = "users_email_key";
    private static final String PAYMENTS_CARDS_NUMBER_KEY_CONSTRAINT = "payments_cards_number_key";

    private static final String NUMBERS_PROPERTY = "numbers";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException e,
                                                HttpServletRequest request) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        log.debug("Validation failed: {}", fieldErrors);

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, VALIDATION_FAILED_TITLE, buildValidationDetail(fieldErrors), request)
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handle(ConstraintViolationException e,
                                                HttpServletRequest request) {
        String detail = e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));

        log.debug("Constraint violation: {}", detail);

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, VALIDATION_FAILED_TITLE, detail, request)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handle(HandlerMethodValidationException e,
                                                HttpServletRequest request) {
        String detail = e.getAllErrors().stream()
            .map(error -> error.getDefaultMessage() == null ? error.toString() : error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));

        log.debug("Handler method validation failed: {}", detail);

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, VALIDATION_FAILED_TITLE, detail, request)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handle(HttpMessageNotReadableException e,
                                                HttpServletRequest request) {
        log.debug("Malformed request body: {}", e.getMessage());

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, MALFORMED_REQUEST_BODY_TITLE, MALFORMED_REQUEST_BODY_DETAIL, request)
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(UserNotFoundException e,
                                                HttpServletRequest request) {
        log.debug("User not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            problem(NOT_FOUND, USER_NOT_FOUND_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(PaymentCardNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(PaymentCardNotFoundException e,
                                                HttpServletRequest request) {
        log.debug("Payment card not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            problem(NOT_FOUND, PAYMENT_CARD_NOT_FOUND_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(UserEmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handle(UserEmailAlreadyExistsException e,
                                                HttpServletRequest request) {
        log.debug("User email already exists: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, USER_EMAIL_ALREADY_EXISTS_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(PaymentCardNumberAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handle(PaymentCardNumberAlreadyExistsException e,
                                                HttpServletRequest request) {
        log.debug("Payment card number already exists: {}", e.getMessage());

        ProblemDetail problem = problem(
            CONFLICT,
            PAYMENT_CARD_ALREADY_EXISTS_TITLE,
            e.getMessage(),
            request
        );
        problem.setProperty(NUMBERS_PROPERTY, e.getNumbers());

        return ResponseEntity.status(CONFLICT).body(problem);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handle(DataIntegrityViolationException e,
                                                HttpServletRequest request) {
        String rootMessage = e.getMostSpecificCause().getMessage();

        String title = CONFLICT_TITLE;
        String detail = CONFLICT_DETAIL;

        if (rootMessage != null) {
            if (rootMessage.contains(USERS_EMAIL_KEY_CONSTRAINT)) {
                title = USER_EMAIL_ALREADY_EXISTS_TITLE;
                detail = USER_EMAIL_ALREADY_EXISTS_TITLE;
            } else if (rootMessage.contains(PAYMENTS_CARDS_NUMBER_KEY_CONSTRAINT)) {
                title = PAYMENT_CARD_ALREADY_EXISTS_TITLE;
                detail = PAYMENT_CARD_ALREADY_EXISTS_TITLE;
            }
        }

        log.debug("Data integrity violation: {}", rootMessage);

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, title, detail, request)
        );
    }

    @ExceptionHandler(UserPaymentCardsLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handle(UserPaymentCardsLimitExceededException e,
                                                HttpServletRequest request) {
        log.debug("User payment cards limit exceeded: {}", e.getMessage());

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, USER_PAYMENT_CARDS_LIMIT_EXCEEDED_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(PaymentCardOwnershipException.class)
    public ResponseEntity<ProblemDetail> handle(PaymentCardOwnershipException e,
                                                HttpServletRequest request) {
        log.debug("Payment card ownership violation: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, PAYMENT_CARD_OWNERSHIP_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(UserAlreadyActiveException.class)
    public ResponseEntity<ProblemDetail> handle(UserAlreadyActiveException e,
                                                HttpServletRequest request) {
        log.debug("User already active: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, USER_ALREADY_ACTIVE_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ResponseEntity<ProblemDetail> handle(UserAlreadyInactiveException e,
                                                HttpServletRequest request) {
        log.debug("User already inactive: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, USER_ALREADY_INACTIVE_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(PaymentCardAlreadyActiveException.class)
    public ResponseEntity<ProblemDetail> handle(PaymentCardAlreadyActiveException e,
                                                HttpServletRequest request) {
        log.debug("Payment card already active: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, PAYMENT_CARD_ALREADY_ACTIVE_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(PaymentCardAlreadyInactiveException.class)
    public ResponseEntity<ProblemDetail> handle(PaymentCardAlreadyInactiveException e,
                                                HttpServletRequest request) {
        log.debug("Payment card already inactive: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, PAYMENT_CARD_ALREADY_INACTIVE_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ProblemDetail> handle(InvalidCursorException e,
                                                HttpServletRequest request) {
        log.debug("Invalid cursor: {}", e.getMessage());

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, INVALID_CURSOR_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handle(AccessDeniedException e,
                                                HttpServletRequest request) {
        log.debug("Access denied: {}", e.getMessage());

        return ResponseEntity.status(FORBIDDEN).body(
            problem(FORBIDDEN, ACCESS_DENIED_TITLE, ACCESS_DENIED_DETAIL, request)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handle(Exception e,
                                                HttpServletRequest request) {
        log.error("Unhandled exception", e);

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(
            problem(INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_TITLE, INTERNAL_SERVER_ERROR_DETAIL, request)
        );
    }

    private ProblemDetail problem(HttpStatus status,
                                  String title,
                                  String detail,
                                  HttpServletRequest request) {
        return ProblemDetailsFactory.build(status, title, detail, request);
    }

    private String buildValidationDetail(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
    }

}
