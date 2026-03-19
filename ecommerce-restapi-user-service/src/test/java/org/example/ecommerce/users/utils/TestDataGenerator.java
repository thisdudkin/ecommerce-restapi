package org.example.ecommerce.users.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.request.UserRequest;
import org.example.ecommerce.users.dto.request.UserUpdateRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.dto.response.UserListResponse;
import org.example.ecommerce.users.dto.response.UserResponse;
import org.example.ecommerce.users.entity.PaymentCard;
import org.example.ecommerce.users.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public final class TestDataGenerator {

    private static final RandomStringUtils RSU = RandomStringUtils.insecure();

    private TestDataGenerator() {
    }

    public static Long id() {
        return ThreadLocalRandom.current().nextLong(1, 10_000);
    }

    public static String name() {
        return "Name-" + RSU.nextAlphabetic(8);
    }

    public static String surname() {
        return "Surname-" + RSU.nextAlphabetic(10);
    }

    public static String email() {
        return RSU.nextAlphabetic(10).toLowerCase()
            + RSU.nextNumeric(3)
            + "@test.com";
    }

    public static String cardNumber() {
        return RSU.nextNumeric(16);
    }

    public static String cardHolder() {
        return name() + " " + surname();
    }

    public static LocalDate birthdate() {
        return LocalDate.now().minusYears(
            ThreadLocalRandom.current().nextInt(18, 60)
        );
    }

    public static LocalDate expirationDate() {
        return LocalDate.now().plusYears(
            ThreadLocalRandom.current().nextInt(1, 6)
        );
    }

    public static LocalDateTime datetime() {
        return LocalDateTime.now().minusDays(
            ThreadLocalRandom.current().nextInt(1, 365)
        );
    }

    public static User user() {
        User user = User.builder()
            .name(name())
            .surname(surname())
            .birthDate(birthdate())
            .email(email())
            .active(true)
            .build();

        user.setId(id());
        user.setCreatedAt(datetime());
        user.setUpdatedAt(datetime());
        return user;
    }

    public static User user(Long id) {
        User user = user();
        user.setId(id);
        return user;
    }

    public static PaymentCard paymentCard() {
        PaymentCard card = PaymentCard.builder()
            .number(cardNumber())
            .holder(cardHolder())
            .expirationDate(expirationDate())
            .active(true)
            .build();

        card.setId(id());
        card.setCreatedAt(datetime());
        card.setUpdatedAt(datetime());
        return card;
    }

    public static PaymentCard paymentCard(Long id) {
        PaymentCard card = paymentCard();
        card.setId(id);
        return card;
    }

    public static PaymentCard paymentCard(User user) {
        PaymentCard card = paymentCard();
        card.setUser(user);
        return card;
    }

    public static UserRequest userRequest() {
        return new UserRequest(
            name(),
            surname(),
            birthdate(),
            email(),
            Collections.emptyList()
        );
    }

    public static UserRequest userRequest(List<PaymentCardRequest> cards) {
        return new UserRequest(
            name(),
            surname(),
            birthdate(),
            email(),
            cards
        );
    }

    public static UserUpdateRequest userUpdateRequest() {
        return new UserUpdateRequest(
            name(),
            surname(),
            birthdate(),
            email()
        );
    }

    public static PaymentCardRequest paymentCardRequest() {
        return new PaymentCardRequest(
            cardNumber(),
            cardHolder(),
            expirationDate()
        );
    }

    public static PaymentCardResponse paymentCardResponse() {
        return new PaymentCardResponse(
            id(),
            cardNumber(),
            cardHolder(),
            expirationDate(),
            Boolean.TRUE,
            datetime(),
            datetime()
        );
    }

    public static UserResponse userResponse() {
        return new UserResponse(
            id(),
            name(),
            surname(),
            birthdate(),
            email(),
            Boolean.TRUE,
            Collections.emptySet(),
            datetime(),
            datetime()
        );
    }

    public static UserResponse userResponse(Set<PaymentCardResponse> cards) {
        return new UserResponse(
            id(),
            name(),
            surname(),
            birthdate(),
            email(),
            Boolean.TRUE,
            cards,
            datetime(),
            datetime()
        );
    }

    public static List<PaymentCardRequest> paymentCardRequests(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> paymentCardRequest())
            .toList();
    }

    public static Set<PaymentCard> paymentCards(int count, User user) {
        Set<PaymentCard> cards = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            PaymentCard card = paymentCard(user);
            cards.add(card);
            user.addCard(card);
        }
        return cards;
    }

    public static UserListResponse userListResponse() {
        return new UserListResponse(
            id(),
            name(),
            surname(),
            birthdate(),
            email(),
            Boolean.TRUE,
            datetime(),
            datetime()
        );
    }

    public static UserListResponse userListResponse(Long id) {
        UserListResponse response = userListResponse();
        return new UserListResponse(
            id,
            response.name(),
            response.surname(),
            response.birthDate(),
            response.email(),
            response.active(),
            response.createdAt(),
            response.updatedAt()
        );
    }

    public static UserListResponse userListResponse(User user) {
        return new UserListResponse(
            user.getId(),
            user.getName(),
            user.getSurname(),
            user.getBirthDate(),
            user.getEmail(),
            user.getActive(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

}
