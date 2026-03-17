package org.example.ecommerce.auth.exception.handler;

import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.ecommerce.auth.exception.custom.*;
import org.example.ecommerce.auth.security.exception.InvalidJwtException;
import org.example.ecommerce.auth.security.exception.InvalidRefreshTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final String VALIDATION_FAILED_TITLE = "Validation failed";
    private static final String VALIDATION_FAILED_CODE = "VALIDATION_FAILED";

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
                                                                                  HttpServletRequest request) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        log.debug("Validation failed: {}", fieldErrors);

        return ResponseEntity.status(BAD_REQUEST).body(
            problem(BAD_REQUEST, VALIDATION_FAILED_TITLE, buildValidationDetail(fieldErrors), VALIDATION_FAILED_CODE, request)
                .withErrors(toFieldViolation(fieldErrors))
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
            .map(error -> error.getDefaultMessage() == null
                ? error.toString()
                : error.getDefaultMessage())
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
                "Malformed request body",
                "Request body is missing or has invalid JSON format",
                "MALFORMED_REQUEST_BODY",
                request
            )
        );
    }

    @ExceptionHandler(CredentialAlreadyExistsException.class)
    public ResponseEntity<ApiProblemDetail> handleCredentialAlreadyExistsException(CredentialAlreadyExistsException e,
                                                                                   HttpServletRequest request) {
        log.debug("Credential already exists: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, "Credential already exists", e.getMessage(), "CREDENTIAL_ALREADY_EXISTS", request)
        );
    }

    @ExceptionHandler(CredentialNotFoundException.class)
    public ResponseEntity<ApiProblemDetail> handleCredentialNotFoundException(CredentialNotFoundException e,
                                                                              HttpServletRequest request) {
        log.debug("Credential not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            problem(NOT_FOUND, "Credential not found", e.getMessage(), "CREDENTIAL_NOT_FOUND", request)
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiProblemDetail> handleBadCredentialsException(BadCredentialsException e,
                                                                          HttpServletRequest request) {
        log.debug("Authentication failed", e);

        return ResponseEntity.status(UNAUTHORIZED).body(
            problem(
                UNAUTHORIZED,
                "Authentication failed",
                "Invalid login or password",
                "AUTHENTICATION_FAILED",
                request
            )
        );
    }

    @ExceptionHandler({InactiveUserCredentialException.class, DisabledException.class})
    public ResponseEntity<ApiProblemDetail> handleInactiveCredentialException(Exception e,
                                                                              HttpServletRequest request) {
        log.debug("Credential inactive: {}", e.getMessage());

        return ResponseEntity.status(FORBIDDEN).body(
            problem(
                FORBIDDEN,
                "Credential inactive",
                "Credential is inactive",
                "CREDENTIAL_INACTIVE",
                request
            )
        );
    }

    @ExceptionHandler({InvalidJwtException.class, InvalidRefreshTokenException.class})
    public ResponseEntity<ApiProblemDetail> handleInvalidTokenException(RuntimeException e,
                                                                        HttpServletRequest request) {
        log.debug("Invalid token: {}", e.getMessage());

        return ResponseEntity.status(UNAUTHORIZED).body(
            problem(
                UNAUTHORIZED,
                "Invalid token",
                e.getMessage(),
                "INVALID_TOKEN",
                request
            )
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiProblemDetail> handleUserAlreadyExistsException(UserAlreadyExistsException e,
                                                                             HttpServletRequest request) {
        log.debug("User already exists: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, e.getTitle(), e.getMessage(), "USER_ALREADY_EXISTS", request)
        );
    }

    @ExceptionHandler({DownstreamServiceUnavailableException.class, RetryableException.class})
    public ResponseEntity<ApiProblemDetail> handleDownstreamServiceUnavailableException(Exception e,
                                                                                        HttpServletRequest request) {
        log.warn("Downstream service unavailable: {}", e.getMessage());

        return ResponseEntity.status(SERVICE_UNAVAILABLE).body(
            problem(
                SERVICE_UNAVAILABLE,
                "Service unavailable",
                "Dependent service is temporarily unavailable",
                "DOWNSTREAM_SERVICE_UNAVAILABLE",
                request
            )
        );
    }

    @ExceptionHandler(CompensationFailedException.class)
    public ResponseEntity<ApiProblemDetail> handleCompensationFailedException(CompensationFailedException e,
                                                                              HttpServletRequest request) {
        log.warn("Registration partially failed: {}", e.getMessage());

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(
            problem(
                INTERNAL_SERVER_ERROR,
                "Registration partially failed",
                "User was created in user-service, but cleanup could not be completed immediately. The incident was saved for retry.",
                "REGISTRATION_COMPENSATION_FAILED",
                request
            )
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiProblemDetail> handleAccessDeniedException(AccessDeniedException e,
                                                                        HttpServletRequest request) {
        log.debug("Access denied: {}", e.getMessage());

        return ResponseEntity.status(FORBIDDEN).body(
            problem(
                FORBIDDEN,
                "Access denied",
                "You do not have permission to access this resource",
                "ACCESS_DENIED",
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
                "Internal server error",
                "An unexpected error occurred",
                "INTERNAL_SERVER_ERROR",
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

    private List<ApiProblemDetail.FieldViolation> toFieldViolation(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
            .map(error -> new ApiProblemDetail.FieldViolation(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .toList();
    }

}
