package org.example.ecommerce.users.exception.custom;

public class PaymentCardOwnershipException extends RuntimeException {
    public PaymentCardOwnershipException(Long cardId, Long userId) {
        super("Payment card with id " + cardId + " does not belong to user with id " + userId);
    }
}
