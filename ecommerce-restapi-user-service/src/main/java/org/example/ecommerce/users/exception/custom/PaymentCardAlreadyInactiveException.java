package org.example.ecommerce.users.exception.custom;

public class PaymentCardAlreadyInactiveException extends RuntimeException {
    public PaymentCardAlreadyInactiveException(Long cardId) {
        super("PaymentCard with id " + cardId + " is already inactive.");
    }
}
