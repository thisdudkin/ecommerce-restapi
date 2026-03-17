package org.example.ecommerce.auth.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

public class ApiProblemDetail extends ProblemDetail implements Serializable {

    private final Instant timestamp;
    private final String path;
    private String errorCode;
    private List<FieldViolation> errors;

    private ApiProblemDetail(HttpStatus status,
                             String title,
                             String detail,
                             String path) {
        super(status.value());
        setTitle(title);
        setDetail(detail);
        this.timestamp = Instant.now();
        this.path = path;
    }

    public static ApiProblemDetail of(HttpStatus status,
                                      String title,
                                      String detail,
                                      String path) {
        return new ApiProblemDetail(status, title, detail, path);
    }

    public ApiProblemDetail withErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public ApiProblemDetail withErrors(List<FieldViolation> errors) {
        this.errors = (errors == null || errors.isEmpty()) ? null : List.copyOf(errors);
        return this;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<FieldViolation> getErrors() {
        return errors;
    }

    public record FieldViolation(
        String field,
        String message,
        Object rejectedValue
    ) implements Serializable {
    }
}
