package org.example.ecommerce.auth.exception.custom;

public class UserAlreadyExistsException extends RuntimeException {
    private final String title;
    private final String detail;

    public UserAlreadyExistsException(String title, String detail) {
        super(detail);
        this.title = title;
        this.detail = detail;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getMessage() {
        return detail;
    }
}
