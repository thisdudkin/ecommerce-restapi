package org.example.ecommerce.users.exception.custom;

public class PaymentCardNotFoundException extends RuntimeException {
    public PaymentCardNotFoundException(Long cardId) {
        super("Payment card not found with id: " + cardId);
    }
}
