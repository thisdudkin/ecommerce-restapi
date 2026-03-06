package org.example.ecommerce.users.exception.custom;

public class DuplicatePaymentCardNumbersException extends RuntimeException {
    public DuplicatePaymentCardNumbersException() {
        super("Request contains duplicate payment card numbers");
    }
}
