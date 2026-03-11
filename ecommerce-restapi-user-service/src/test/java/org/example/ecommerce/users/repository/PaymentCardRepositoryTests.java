package org.example.ecommerce.users.repository;

import org.example.ecommerce.users.entity.PaymentCard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Sql("classpath:insert.sql")
@AutoConfigureTestDatabase(replace = NONE)
class PaymentCardRepositoryTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.0");

    @Autowired
    private PaymentCardRepository repository;

    @Test
    void findByIdWithUserShouldReturnCardWithUser() {
        // Act
        PaymentCard card = repository.findByIdWithUser(1L)
            .orElseThrow();

        // Assert
        assertThat(card.getHolder()).isEqualTo("ALEXANDER DUDKIN");
        assertThat(card.getUser()).isNotNull();
        assertThat(card.getUser().getSurname()).isEqualTo("Dudkin");
    }

    @Test
    void findByIdWithUserShouldReturnEmptyWhenCardNotExist() {
        // Act
        Optional<PaymentCard> card = repository.findByIdWithUser(999L);

        // Assert
        assertThat(card).isEmpty();
    }

    @Test
    void findAllByUserIdShouldReturnAllCardsForUser() {
        // Act
        List<PaymentCard> cards = repository.findAllByUserId(1L);

        // Assert
        assertThat(cards)
            .hasSize(3)
            .allMatch(card -> card.getUser().getId().equals(1L));
    }

    @Test
    void findAllByUserIdShouldReturnEmptyWhenUserHasNoCards() {
        // Act
        List<PaymentCard> cards = repository.findAllByUserId(999L);

        // Assert
        assertThat(cards).isEmpty();
    }

    @Test
    void countByIdShouldReturnCorrectCount() {
        // Act
        long count = repository.countByUserId(5L);

        // Assert
        assertThat(count).isEqualTo(5);
    }

    @Test
    void countByIdShouldReturnZeroWhenUserHasNoCards() {
        // Act
        long count = repository.countByUserId(999L);

        // Assert
        assertThat(count).isZero();
    }

    @Test
    void updateActiveStatusShouldUpdateCard() {
        // Act
        int rows = repository.updateActiveStatus(1L, Boolean.FALSE);

        // Assert
        PaymentCard card = repository.findById(1L)
            .orElseThrow();

        assertThat(rows).isEqualTo(1);
        assertThat(card.getActive()).isFalse();
    }

    @Test
    void updateActiveStatusShouldReturnZeroWhenCardNotExist() {
        // Act
        int rows = repository.updateActiveStatus(999L, Boolean.FALSE);

        // Assert
        assertThat(rows).isZero();
    }

    @Test
    void existsByNumberShouldReturnTrueWhenCardNumberExists() {
        // Act
        boolean exists = repository.existsByNumber("4112-8453-7741-9021");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByNumberShouldReturnFalseWhenCardNumberDoesNotExist() {
        // Act
        boolean exists = repository.existsByNumber("0000-0000-0000-0000");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void findExistingNumbersShouldReturnOnlyExistingNumbers() {
        // Act
        Set<String> existingNumbers = repository.findExistingNumbers(List.of(
            "4112-8453-7741-9021",
            "0000-0000-0000-0000",
            "1111-2222-3333-4444"
        ));

        // Assert
        assertThat(existingNumbers).containsExactly("4112-8453-7741-9021");
    }

    @Test
    void findExistingNumbersShouldReturnAllExistingNumbers() {
        // Act
        Set<String> existingNumbers = repository.findExistingNumbers(List.of(
            "4112-8453-7741-9021",
            "5314-6621-1943-7752"
        ));

        // Assert
        assertThat(existingNumbers).containsExactlyInAnyOrder(
            "4112-8453-7741-9021",
            "5314-6621-1943-7752"
        );
    }

    @Test
    void findExistingNumbersShouldReturnEmptySetWhenNoNumbersExist() {
        // Act
        Set<String> existingNumbers = repository.findExistingNumbers(List.of(
            "0000-0000-0000-0000",
            "9999-9999-9999-9999"
        ));

        // Assert
        assertThat(existingNumbers).isEmpty();
    }

}
