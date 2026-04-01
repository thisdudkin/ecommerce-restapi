package org.example.ecommerce.orders.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.example.ecommerce.orders.exception.custom.feign.UserNotFoundException;
import org.example.ecommerce.orders.exception.custom.feign.UserServiceUnavailableException;
import org.example.ecommerce.orders.exception.custom.item.ItemNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.EmptyOrderException;
import org.example.ecommerce.orders.exception.custom.order.OrderItemNotFoundInOrderException;
import org.example.ecommerce.orders.exception.custom.order.OrderNotFoundException;
import org.example.ecommerce.orders.exception.custom.order.OrderStateConflictException;
import org.example.ecommerce.orders.exception.custom.pagination.InvalidCursorException;
import org.example.ecommerce.orders.exception.utils.ProblemDetailsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.DETAIL_ACCESS_DENIED;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.DETAIL_INTERNAL_SERVER_ERROR;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.DETAIL_MALFORMED_REQUEST_BODY;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_ACCESS_DENIED;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_BAD_REQUEST;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_EMPTY_ORDER;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_INTERNAL_SERVER_ERROR;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_INVALID_CURSOR;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_ITEM_NOT_FOUND;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_MALFORMED_REQUEST_BODY;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_ORDER_ITEM_NOT_FOUND;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_ORDER_NOT_FOUND;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_ORDER_STATE_CONFLICT;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_USER_NOT_FOUND;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_USER_SERVICE_UNAVAILABLE;
import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.TITLE_VALIDATION_FAILED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String detail = fieldErrors.stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));

        log.debug("Validation failed: {}", detail);

        return ResponseEntity.badRequest().body(
            ProblemDetailsFactory.build(BAD_REQUEST, TITLE_VALIDATION_FAILED, detail, request)
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handle(ConstraintViolationException e, HttpServletRequest request) {
        String detail = e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .sorted()
            .collect(Collectors.joining("; "));

        log.debug("Constraint violation: {}", detail);

        return ResponseEntity.badRequest().body(
            ProblemDetailsFactory.build(BAD_REQUEST, TITLE_VALIDATION_FAILED, detail, request)
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handle(HandlerMethodValidationException e, HttpServletRequest request) {
        String detail = e.getAllErrors().stream()
            .map(error -> error.getDefaultMessage() == null
                ? error.toString()
                : error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));

        log.debug("Handler method validation failed: {}", detail);

        return ResponseEntity.badRequest().body(
            ProblemDetailsFactory.build(BAD_REQUEST, TITLE_VALIDATION_FAILED, detail, request)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handle(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.debug("Malformed request body: {}", e.getMessage());

        return ResponseEntity.badRequest().body(
            ProblemDetailsFactory.build(BAD_REQUEST, TITLE_MALFORMED_REQUEST_BODY, DETAIL_MALFORMED_REQUEST_BODY, request)
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handle(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String detail = "Invalid value '%s' for parameter '%s'".formatted(e.getValue(), e.getName());

        log.debug("Method argument type mismatch: {}", detail);

        return ResponseEntity.badRequest().body(
            ProblemDetailsFactory.build(BAD_REQUEST, TITLE_BAD_REQUEST, detail, request)
        );
    }

    @ExceptionHandler(ItemNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(ItemNotFoundException e, HttpServletRequest request) {
        log.debug("Item not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            ProblemDetailsFactory.build(NOT_FOUND, TITLE_ITEM_NOT_FOUND, e.getMessage(), request)
        );
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(OrderNotFoundException e, HttpServletRequest request) {
        log.debug("Order not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            ProblemDetailsFactory.build(NOT_FOUND, TITLE_ORDER_NOT_FOUND, e.getMessage(), request)
        );
    }

    @ExceptionHandler(OrderItemNotFoundInOrderException.class)
    public ResponseEntity<ProblemDetail> handle(OrderItemNotFoundInOrderException e, HttpServletRequest request) {
        log.debug("Order item not found in order: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            ProblemDetailsFactory.build(NOT_FOUND, TITLE_ORDER_ITEM_NOT_FOUND, e.getMessage(), request)
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handle(UserNotFoundException e, HttpServletRequest request) {
        log.debug("User not found: {}", e.getMessage());

        return ResponseEntity.status(NOT_FOUND).body(
            ProblemDetailsFactory.build(NOT_FOUND, TITLE_USER_NOT_FOUND, e.getMessage(), request)
        );
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ProblemDetail> handle(InvalidCursorException e, HttpServletRequest request) {
        log.debug("Invalid cursor: {}", e.getMessage());

        return ResponseEntity.badRequest().body(
            ProblemDetailsFactory.build(BAD_REQUEST, TITLE_INVALID_CURSOR, e.getMessage(), request)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handle(IllegalArgumentException e, HttpServletRequest request) {
        log.debug("Bad request: {}", e.getMessage());

        return ResponseEntity.badRequest().body(
            ProblemDetailsFactory.build(BAD_REQUEST, TITLE_BAD_REQUEST, e.getMessage(), request)
        );
    }

    @ExceptionHandler(EmptyOrderException.class)
    public ResponseEntity<ProblemDetail> handle(EmptyOrderException e, HttpServletRequest request) {
        log.debug("Empty order: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            ProblemDetailsFactory.build(CONFLICT, TITLE_EMPTY_ORDER, e.getMessage(), request)
        );
    }

    @ExceptionHandler(OrderStateConflictException.class)
    public ResponseEntity<ProblemDetail> handle(OrderStateConflictException e, HttpServletRequest request) {
        log.debug("Order state conflict: {}", e.getMessage());

        return ResponseEntity.status(CONFLICT).body(
            ProblemDetailsFactory.build(CONFLICT, TITLE_ORDER_STATE_CONFLICT, e.getMessage(), request)
        );
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handle(UserServiceUnavailableException e, HttpServletRequest request) {
        log.debug("User service unavailable: {}", e.getMessage());

        return ResponseEntity.status(SERVICE_UNAVAILABLE).body(
            ProblemDetailsFactory.build(SERVICE_UNAVAILABLE, TITLE_USER_SERVICE_UNAVAILABLE, e.getMessage(), request)
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handle(AccessDeniedException e, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ProblemDetailsFactory.build(HttpStatus.FORBIDDEN, TITLE_ACCESS_DENIED, DETAIL_ACCESS_DENIED, request)
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handle(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);

        return ResponseEntity.internalServerError().body(
            ProblemDetailsFactory.build(INTERNAL_SERVER_ERROR, TITLE_INTERNAL_SERVER_ERROR, DETAIL_INTERNAL_SERVER_ERROR, request)
        );
    }

}
