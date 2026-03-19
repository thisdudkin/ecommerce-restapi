package org.example.ecommerce.users.exception.custom;

public class PaymentCardAlreadyActiveException extends RuntimeException {
    public PaymentCardAlreadyActiveException(Long cardId) {
        super("PaymentCard with id " + cardId + " is already active.");
    }
}
