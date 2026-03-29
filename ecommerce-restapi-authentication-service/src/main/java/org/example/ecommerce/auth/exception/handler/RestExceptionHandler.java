package org.example.ecommerce.auth.exception.handler;

import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.ecommerce.auth.exception.custom.CompensationFailedException;
import org.example.ecommerce.auth.exception.custom.CredentialAlreadyExistsException;
import org.example.ecommerce.auth.exception.custom.CredentialNotFoundException;
import org.example.ecommerce.auth.exception.custom.DownstreamServiceUnavailableException;
import org.example.ecommerce.auth.exception.custom.InactiveUserCredentialException;
import org.example.ecommerce.auth.exception.custom.UserAlreadyExistsException;
import org.example.ecommerce.auth.exception.utils.ProblemDetailsFactory;
import org.example.ecommerce.auth.security.exception.InvalidJwtException;
import org.example.ecommerce.auth.security.exception.InvalidRefreshTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.ACCESS_DENIED_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.ACCESS_DENIED_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.AUTHENTICATION_FAILED_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.AUTHENTICATION_FAILED_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.CREDENTIAL_ALREADY_EXISTS_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.CREDENTIAL_INACTIVE_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.CREDENTIAL_INACTIVE_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.CREDENTIAL_NOT_FOUND_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.INTERNAL_SERVER_ERROR_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.INTERNAL_SERVER_ERROR_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.INVALID_TOKEN_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.MALFORMED_REQUEST_BODY_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.MALFORMED_REQUEST_BODY_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.REGISTRATION_PARTIALLY_FAILED_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.REGISTRATION_PARTIALLY_FAILED_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.SERVICE_UNAVAILABLE_DETAIL;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.SERVICE_UNAVAILABLE_TITLE;
import static org.example.ecommerce.auth.exception.utils.ProblemDetailsConstants.VALIDATION_FAILED_TITLE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

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

    @ExceptionHandler(CredentialAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handle(CredentialAlreadyExistsException e,
                                                HttpServletRequest request) {
        log.debug("Credential already exists: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, CREDENTIAL_ALREADY_EXISTS_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(CredentialNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(CredentialNotFoundException e,
                                                HttpServletRequest request) {
        log.debug("Credential not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            problem(NOT_FOUND, CREDENTIAL_NOT_FOUND_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handle(BadCredentialsException e,
                                                HttpServletRequest request) {
        log.debug("Authentication failed", e);

        return ResponseEntity.status(UNAUTHORIZED).body(
            problem(UNAUTHORIZED, AUTHENTICATION_FAILED_TITLE, AUTHENTICATION_FAILED_DETAIL, request)
        );
    }

    @ExceptionHandler({InactiveUserCredentialException.class, DisabledException.class})
    public ResponseEntity<ProblemDetail> handleDisabled(Exception e,
                                                        HttpServletRequest request) {
        log.debug("Credential inactive: {}", e.getMessage());

        return ResponseEntity.status(FORBIDDEN).body(
            problem(FORBIDDEN, CREDENTIAL_INACTIVE_TITLE, CREDENTIAL_INACTIVE_DETAIL, request)
        );
    }

    @ExceptionHandler({InvalidJwtException.class, InvalidRefreshTokenException.class})
    public ResponseEntity<ProblemDetail> handle(RuntimeException e,
                                                HttpServletRequest request) {
        log.debug("Invalid token: {}", e.getMessage());

        return ResponseEntity.status(UNAUTHORIZED).body(
            problem(UNAUTHORIZED, INVALID_TOKEN_TITLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handle(UserAlreadyExistsException e,
                                                HttpServletRequest request) {
        log.debug("User already exists: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            problem(CONFLICT, e.getTitle(), e.getMessage(), request)
        );
    }

    @ExceptionHandler({DownstreamServiceUnavailableException.class, RetryableException.class})
    public ResponseEntity<ProblemDetail> handleDownstream(Exception e,
                                                          HttpServletRequest request) {
        log.warn("Downstream service unavailable: {}", e.getMessage());

        return ResponseEntity.status(SERVICE_UNAVAILABLE).body(
            problem(SERVICE_UNAVAILABLE, SERVICE_UNAVAILABLE_TITLE, SERVICE_UNAVAILABLE_DETAIL, request)
        );
    }

    @ExceptionHandler(CompensationFailedException.class)
    public ResponseEntity<ProblemDetail> handle(CompensationFailedException e,
                                                HttpServletRequest request) {
        log.warn("Registration partially failed: {}", e.getMessage());

        return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(
            problem(
                INTERNAL_SERVER_ERROR,
                REGISTRATION_PARTIALLY_FAILED_TITLE,
                REGISTRATION_PARTIALLY_FAILED_DETAIL,
                request
            )
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
