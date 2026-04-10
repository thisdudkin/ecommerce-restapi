package org.example.ecommerce.payments.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.ecommerce.payments.domain.exception.DomainException;
import org.example.ecommerce.payments.infrastructure.exception.InfrastructureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handle(DomainException e) {
        log.info("Domain exception: {}", e.getMessage());

        return build(e.getStatus(), e.getTitle(), e.getMessage());
    }

    @ExceptionHandler(InfrastructureException.class)
    public ResponseEntity<ProblemDetail> handle(InfrastructureException e) {
        log.warn("Infrastructure exception: {}", e.getMessage(), e);

        return build(e.getStatus(), e.getTitle(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

        String detail = fieldErrors.stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));

        log.debug("Validation failed: {}", detail);

        return build(
            HttpStatus.BAD_REQUEST,
            "Validation failed",
            detail
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handle(HttpMessageNotReadableException e) {
        log.debug("Malformed request body: {}", e.getMessage());

        return build(
            HttpStatus.BAD_REQUEST,
            "Malformed request body",
            "Request body is missing or has invalid JSON format"
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentTypeMismatchException e) {
        String detail = "Parameter '%s' has invalid value '%s'"
            .formatted(e.getName(), e.getValue());

        log.debug("Request parameter type mismatch: {}", detail);

        return build(
            HttpStatus.BAD_REQUEST,
            "Invalid request parameter",
            detail
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handle(Exception e) {
        log.error("Unhandled exception", e);

        return build(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred"
        );
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return ResponseEntity.status(status).body(problemDetail);
    }

}
