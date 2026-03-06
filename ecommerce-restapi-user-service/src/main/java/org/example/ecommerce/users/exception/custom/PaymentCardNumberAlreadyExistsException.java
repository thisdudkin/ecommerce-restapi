package org.example.ecommerce.users.exception.custom;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class PaymentCardNumberAlreadyExistsException extends RuntimeException {
    private final Set<String> numbers;

    public PaymentCardNumberAlreadyExistsException(String number) {
        this(Set.of(number));
    }

    public PaymentCardNumberAlreadyExistsException(Collection<String> numbers) {
        super(buildMessage(numbers));
        this.numbers = Collections.unmodifiableSet(new LinkedHashSet<>(numbers));
    }

    public Set<String> getNumbers() {
        return numbers;
    }

    private static String buildMessage(Collection<String> numbers) {
        return "Payment card number(s) already exist: " + String.join(", ", numbers);
    }

}
