package org.example.ecommerce.users.exception.custom;

public class UserPaymentCardsLimitExceededException extends RuntimeException {
    private final static int LIMIT = 5;

    public UserPaymentCardsLimitExceededException() {
        super("User can have at most " + LIMIT + " payment cards.");
    }
}
