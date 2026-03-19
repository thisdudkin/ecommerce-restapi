package org.example.ecommerce.users.exception.handler;

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
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RestExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();

        String detail = fieldErrors.stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));

        LOG.warn("Validation failed: {}", detail);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, detail);
        problemDetail.setTitle("Validation failed");
        problemDetail.setProperty(
            "errors",
            fieldErrors.stream()
                .map(error -> {
                    Map<String, Object> errorBody = new LinkedHashMap<>();
                    errorBody.put("field", error.getField());
                    errorBody.put("message", error.getDefaultMessage());
                    errorBody.put("rejectedValue", error.getRejectedValue());
                    return errorBody;
                })
                .toList()
        );

        return ResponseEntity.status(BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException e) {
        LOG.warn("Constraint violation: {}", e.getMessage());

        String detail = e.getConstraintViolations().stream()
            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
            .sorted()
            .collect(java.util.stream.Collectors.joining("; "));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, detail);
        problemDetail.setTitle("Validation failed");

        return ResponseEntity.status(BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFoundException(UserNotFoundException e) {
        LOG.warn("User not found: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_FOUND, e.getMessage());
        problemDetail.setTitle("User not found");

        return ResponseEntity.status(NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(PaymentCardNotFoundException.class)
    public ResponseEntity<ProblemDetail> handlePaymentCardNotFoundException(PaymentCardNotFoundException e) {
        LOG.warn("Payment card not found: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(NOT_FOUND, e.getMessage());
        problemDetail.setTitle("Payment card not found");

        return ResponseEntity.status(NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(UserEmailAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUserEmailAlreadyExistsException(UserEmailAlreadyExistsException e) {
        LOG.warn("User email already exists: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle("User email already exists");

        return ResponseEntity.status(CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(PaymentCardNumberAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handlePaymentCardNumberAlreadyExistsException(PaymentCardNumberAlreadyExistsException e) {
        LOG.warn("Payment card number already exists: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle("Payment card number already exists");
        problemDetail.setProperty("numbers", e.getNumbers());

        return ResponseEntity.status(CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        LOG.warn("Data integrity violation", e);

        String message = "Data integrity violation";
        String rootMessage = e.getMostSpecificCause().getMessage();

        if (rootMessage != null) {
            if (rootMessage.contains("users_email_key")) {
                message = "User email already exists";
            } else if (rootMessage.contains("payments_cards_number_key")) {
                message = "Payment card number already exists";
            }
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, message);
        problemDetail.setTitle("Conflict");

        return ResponseEntity.status(CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(UserPaymentCardsLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleUserPaymentCardsLimitExceededException(UserPaymentCardsLimitExceededException e) {
        LOG.warn("User payment cards limit exceeded: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getMessage());
        problemDetail.setTitle("User payment cards limit exceeded");

        return ResponseEntity.status(BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(PaymentCardOwnershipException.class)
    public ResponseEntity<ProblemDetail> handlePaymentCardOwnershipException(PaymentCardOwnershipException e) {
        LOG.warn("Payment card ownership violation: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle("Payment card does not belong to user");

        return ResponseEntity.status(CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(UserAlreadyActiveException.class)
    public ResponseEntity<ProblemDetail> handleUserAlreadyActiveException(UserAlreadyActiveException e) {
        LOG.warn("User already active: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle("User already active");

        return ResponseEntity.status(CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(UserAlreadyInactiveException.class)
    public ResponseEntity<ProblemDetail> handleUserAlreadyInactiveException(UserAlreadyInactiveException e) {
        LOG.warn("User already inactive: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle("User already inactive");

        return ResponseEntity.status(CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(PaymentCardAlreadyActiveException.class)
    public ResponseEntity<ProblemDetail> handlePaymentCardAlreadyActiveException(PaymentCardAlreadyActiveException e) {
        LOG.warn("Payment card already active: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle("Payment card already active");

        return ResponseEntity.status(CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(PaymentCardAlreadyInactiveException.class)
    public ResponseEntity<ProblemDetail> handlePaymentCardAlreadyInactiveException(PaymentCardAlreadyInactiveException e) {
        LOG.warn("Payment card already inactive: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(CONFLICT, e.getMessage());
        problemDetail.setTitle("Payment card already inactive");

        return ResponseEntity.status(CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCursorException(InvalidCursorException e) {
        LOG.warn("Invalid cursor: {}", e.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(BAD_REQUEST, e.getMessage());
        problemDetail.setTitle("Invalid cursor");

        return ResponseEntity.status(BAD_REQUEST).body(problemDetail);
    }

}
