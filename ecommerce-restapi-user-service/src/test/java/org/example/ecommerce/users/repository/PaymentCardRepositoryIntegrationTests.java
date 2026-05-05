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

@Testcontainers
@ActiveProfiles("test")
@Sql("classpath:insert.sql")
@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = NONE)
class PaymentCardRepositoryIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.0");

    @Autowired
    private PaymentCardRepository repository;

    @Test
    void findByIdWithUserShouldReturnCardWithUser() {
        PaymentCard card = repository.findByIdWithUser(1L)
            .orElseThrow();

        assertThat(card.getHolder()).isEqualTo("ALEXANDER DUDKIN");
        assertThat(card.getUser()).isNotNull();
        assertThat(card.getUser().getSurname()).isEqualTo("Dudkin");
    }

    @Test
    void findByIdWithUserShouldReturnEmptyWhenCardNotExist() {
        Optional<PaymentCard> card = repository.findByIdWithUser(999L);

        assertThat(card).isEmpty();
    }

    @Test
    void findAllByUserIdShouldReturnAllCardsForUser() {
        List<PaymentCard> cards = repository.findAllByUserId(1L);

        assertThat(cards)
            .hasSize(3)
            .allMatch(card -> card.getUser().getId().equals(1L));
    }

    @Test
    void findAllByUserIdShouldReturnEmptyWhenUserHasNoCards() {
        List<PaymentCard> cards = repository.findAllByUserId(999L);

        assertThat(cards).isEmpty();
    }

    @Test
    void countByIdShouldReturnCorrectCount() {
        long count = repository.countByUserId(5L);

        assertThat(count).isEqualTo(5);
    }

    @Test
    void countByIdShouldReturnZeroWhenUserHasNoCards() {
        long count = repository.countByUserId(999L);

        assertThat(count).isZero();
    }

    @Test
    void updateActiveStatusShouldUpdateCard() {
        int rows = repository.updateActiveStatus(1L, Boolean.FALSE);

        PaymentCard card = repository.findById(1L)
            .orElseThrow();

        assertThat(rows).isEqualTo(1);
        assertThat(card.getActive()).isFalse();
    }

    @Test
    void updateActiveStatusShouldReturnZeroWhenCardNotExist() {
        int rows = repository.updateActiveStatus(999L, Boolean.FALSE);

        assertThat(rows).isZero();
    }

    @Test
    void existsByNumberShouldReturnTrueWhenCardNumberExists() {
        boolean exists = repository.existsByNumber("4112-8453-7741-9021");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByNumberShouldReturnFalseWhenCardNumberDoesNotExist() {
        boolean exists = repository.existsByNumber("0000-0000-0000-0000");

        assertThat(exists).isFalse();
    }

    @Test
    void findExistingNumbersShouldReturnOnlyExistingNumbers() {
        Set<String> existingNumbers = repository.findExistingNumbers(List.of(
            "4112-8453-7741-9021",
            "0000-0000-0000-0000",
            "1111-2222-3333-4444"
        ));

        assertThat(existingNumbers).containsExactly("4112-8453-7741-9021");
    }

    @Test
    void findExistingNumbersShouldReturnAllExistingNumbers() {
        Set<String> existingNumbers = repository.findExistingNumbers(List.of(
            "4112-8453-7741-9021",
            "5314-6621-1943-7752"
        ));

        assertThat(existingNumbers).containsExactlyInAnyOrder(
            "4112-8453-7741-9021",
            "5314-6621-1943-7752"
        );
    }

    @Test
    void findExistingNumbersShouldReturnEmptySetWhenNoNumbersExist() {
        Set<String> existingNumbers = repository.findExistingNumbers(List.of(
            "0000-0000-0000-0000",
            "9999-9999-9999-9999"
        ));

        assertThat(existingNumbers).isEmpty();
    }

}
