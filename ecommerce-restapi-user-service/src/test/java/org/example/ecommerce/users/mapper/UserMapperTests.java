package org.example.ecommerce.users.mapper;

import org.example.ecommerce.users.dto.request.PaymentCardRequest;
import org.example.ecommerce.users.dto.request.UserRequest;
import org.example.ecommerce.users.dto.request.UserUpdateRequest;
import org.example.ecommerce.users.dto.response.PaymentCardResponse;
import org.example.ecommerce.users.dto.response.UserListResponse;
import org.example.ecommerce.users.dto.response.UserResponse;
import org.example.ecommerce.users.entity.PaymentCard;
import org.example.ecommerce.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(classes = {
    UserMapperImpl.class,
    PaymentCardMapperImpl.class
})
class UserMapperTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    void toEntityShouldMapFieldsAndIgnorePaymentCards() {
        UserRequest request = new UserRequest(
            "Alexander",
            "Dudkin",
            LocalDate.of(1995, 7, 14),
            "alex@example.com",
            List.of(
                new PaymentCardRequest("4111 1111 1111 1111", "ALEX", LocalDate.of(2030, 1, 31))
            )
        );

        User entity = userMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo(request.name());
        assertThat(entity.getSurname()).isEqualTo(request.surname());
        assertThat(entity.getBirthDate()).isEqualTo(request.birthDate());
        assertThat(entity.getEmail()).isEqualTo(request.email());

        assertThat(entity.getPaymentCards()).isNotNull().isEmpty();

        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();

        assertThat(entity.getActive()).isTrue();
    }

    @Test
    void updateEntityShouldUpdateFieldsAndKeepPaymentCards() {
        UserUpdateRequest request = new UserUpdateRequest(
            "UpdatedName",
            "UpdatedSurname",
            LocalDate.of(1998, 8, 18),
            "updated@example.com"
        );

        PaymentCard existingCard = new PaymentCard();
        existingCard.setId(77L);
        existingCard.setNumber("5555 5555 5555 5555");
        existingCard.setHolder("OLD HOLDER");
        existingCard.setExpirationDate(LocalDate.of(2029, 10, 31));
        existingCard.setActive(true);

        Set<PaymentCard> existingCards = new LinkedHashSet<>();
        existingCards.add(existingCard);

        User entity = new User();
        entity.setId(5L);
        entity.setName("OldName");
        entity.setSurname("OldSurname");
        entity.setBirthDate(LocalDate.of(1990, 1, 1));
        entity.setEmail("old@example.com");
        entity.setActive(false);
        entity.setCreatedAt(LocalDateTime.of(2024, 1, 1, 10, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 1, 2, 10, 0));
        entity.setPaymentCards(existingCards);

        userMapper.updateEntity(request, entity);

        assertThat(entity.getName()).isEqualTo(request.name());
        assertThat(entity.getSurname()).isEqualTo(request.surname());
        assertThat(entity.getBirthDate()).isEqualTo(request.birthDate());
        assertThat(entity.getEmail()).isEqualTo(request.email());

        assertThat(entity.getPaymentCards()).containsExactly(existingCard);

        assertThat(entity.getId()).isEqualTo(5L);
        assertThat(entity.getActive()).isFalse();
        assertThat(entity.getCreatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
        assertThat(entity.getUpdatedAt()).isEqualTo(LocalDateTime.of(2024, 1, 2, 10, 0));
    }

    @Test
    void toResponseShouldMapEntityToResponse() {
        PaymentCard card = new PaymentCard();
        card.setId(100L);
        card.setNumber("4000 0000 0000 0002");
        card.setHolder("ALEX DUDKIN");
        card.setExpirationDate(LocalDate.of(2031, 6, 30));
        card.setActive(true);
        card.setCreatedAt(LocalDateTime.of(2024, 3, 1, 9, 0));
        card.setUpdatedAt(LocalDateTime.of(2024, 3, 2, 10, 0));

        User entity = new User();
        entity.setId(1L);
        entity.setName("Alexander");
        entity.setSurname("Dudkin");
        entity.setBirthDate(LocalDate.of(1995, 7, 14));
        entity.setEmail("alex@example.com");
        entity.setActive(true);
        entity.setCreatedAt(LocalDateTime.of(2024, 2, 10, 11, 0));
        entity.setUpdatedAt(LocalDateTime.of(2024, 2, 11, 12, 0));
        entity.addCard(card);

        UserResponse response = userMapper.toResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.name()).isEqualTo(entity.getName());
        assertThat(response.surname()).isEqualTo(entity.getSurname());
        assertThat(response.birthDate()).isEqualTo(entity.getBirthDate());
        assertThat(response.email()).isEqualTo(entity.getEmail());
        assertThat(response.active()).isEqualTo(entity.getActive());
        assertThat(response.createdAt()).isEqualTo(entity.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(entity.getUpdatedAt());

        assertThat(response.paymentCards()).hasSize(1);
        PaymentCardResponse mappedCard = response.paymentCards().iterator().next();
        assertThat(mappedCard.id()).isEqualTo(card.getId());
        assertThat(mappedCard.number()).isEqualTo(card.getNumber());
        assertThat(mappedCard.holder()).isEqualTo(card.getHolder());
        assertThat(mappedCard.expirationDate()).isEqualTo(card.getExpirationDate());
        assertThat(mappedCard.active()).isEqualTo(card.getActive());
        assertThat(mappedCard.createdAt()).isEqualTo(card.getCreatedAt());
        assertThat(mappedCard.updatedAt()).isEqualTo(card.getUpdatedAt());
    }

    @Test
    void toListResponseShouldMapEntityToListResponse() {
        User entity = new User();
        entity.setId(2L);
        entity.setName("Ivan");
        entity.setSurname("Petrov");
        entity.setBirthDate(LocalDate.of(1992, 4, 21));
        entity.setEmail("ivan@example.com");
        entity.setActive(false);
        entity.setCreatedAt(LocalDateTime.of(2024, 4, 1, 8, 30));
        entity.setUpdatedAt(LocalDateTime.of(2024, 4, 2, 9, 45));

        UserListResponse response = userMapper.toListResponse(entity);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(entity.getId());
        assertThat(response.name()).isEqualTo(entity.getName());
        assertThat(response.surname()).isEqualTo(entity.getSurname());
        assertThat(response.birthDate()).isEqualTo(entity.getBirthDate());
        assertThat(response.email()).isEqualTo(entity.getEmail());
        assertThat(response.active()).isEqualTo(entity.getActive());
        assertThat(response.createdAt()).isEqualTo(entity.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(entity.getUpdatedAt());
    }
}
