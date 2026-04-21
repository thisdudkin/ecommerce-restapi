package org.example.ecommerce.users.repository;

import org.example.ecommerce.users.dto.request.UserCursorPayload;
import org.example.ecommerce.users.entity.User;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.example.ecommerce.users.repository.specifications.UserSpecifications;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@Sql("classpath:insert.sql")
@AutoConfigureTestDatabase(replace = NONE)
class UserRepositoryTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.0");

    @Autowired
    private UserRepository repository;

    @Test
    void findByIdWithCardsShouldLoadCards() {
        User user = repository.findByIdWithCards(1L)
            .orElseThrow();

        assertThat(user.getName()).isEqualTo("Alexander");
        assertThat(user.getSurname()).isEqualTo("Dudkin");
        assertThat(user.getPaymentCards()).hasSize(3);
    }

    @Test
    void findByIdWithCardsShouldReturnEmptyWhenUserDoesNotExist() {
        Optional<User> undefined = repository.findByIdWithCards(999L);

        assertThat(undefined).isEmpty();
    }

    @Test
    void findPageWithCardsShouldReturnFirstPagePlusOneAndLoadCards() {
        List<User> users = repository.findPageWithCards(
            UserSpecifications.withFilters(null, null),
            2,
            SortDirection.DESC,
            null
        );

        assertThat(users).hasSize(3);
        assertThat(users)
            .extracting(User::getSurname)
            .containsExactly("Nabok", "Kolesneva", "Nechay-Nicevich");

        assertThat(users.get(0).getPaymentCards()).hasSize(5);
        assertThat(users.get(1).getPaymentCards()).hasSize(2);
        assertThat(users.get(2).getPaymentCards()).hasSize(4);
    }

    @Test
    void findPageWithCardsShouldFilterByNameIgnoringCaseAndTrim() {
        List<User> users = repository.findPageWithCards(
            UserSpecifications.withFilters("  alex  ", null),
            10,
            SortDirection.ASC,
            null
        );

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getName()).isEqualTo("Alexander");
        assertThat(users.getFirst().getPaymentCards()).hasSize(3);
    }

    @Test
    void findPageWithCardsShouldFilterBySurnameIgnoringCaseAndTrim() {
        List<User> users = repository.findPageWithCards(
            UserSpecifications.withFilters(null, "  dud  "),
            10,
            SortDirection.ASC,
            null
        );

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getSurname()).isEqualTo("Dudkin");
        assertThat(users.getFirst().getPaymentCards()).hasSize(3);
    }

    @Test
    void findPageWithCardsShouldFilterByNameAndSurname() {
        List<User> users = repository.findPageWithCards(
            UserSpecifications.withFilters("alex", "dud"),
            10,
            SortDirection.ASC,
            null
        );

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getName()).isEqualTo("Alexander");
        assertThat(users.getFirst().getSurname()).isEqualTo("Dudkin");
        assertThat(users.getFirst().getPaymentCards()).hasSize(3);
    }

    @Test
    void findPageWithCardsShouldIgnoreBlankFilters() {
        List<User> users = repository.findPageWithCards(
            UserSpecifications.withFilters("   ", "   "),
            10,
            SortDirection.ASC,
            null
        );

        assertThat(users).hasSize(5);
    }

    @Test
    void findPageWithCardsShouldReturnSecondPageUsingCursor() {
        List<User> firstPage = repository.findPageWithCards(
            UserSpecifications.withFilters(null, null),
            2,
            SortDirection.DESC,
            null
        );

        User lastVisibleUserFromFirstPage = firstPage.get(1);
        UserCursorPayload cursor = new UserCursorPayload(
            lastVisibleUserFromFirstPage.getCreatedAt(),
            lastVisibleUserFromFirstPage.getId(),
            SortDirection.DESC
        );

        List<User> secondPage = repository.findPageWithCards(
            UserSpecifications.withFilters(null, null),
            2,
            SortDirection.DESC,
            cursor
        );

        assertThat(secondPage).hasSize(3);
        assertThat(secondPage)
            .extracting(User::getSurname)
            .containsExactly("Nechay-Nicevich", "Inchakov", "Dudkin");

        assertThat(secondPage.get(0).getPaymentCards()).hasSize(4);
        assertThat(secondPage.get(1).getPaymentCards()).hasSize(1);
        assertThat(secondPage.get(2).getPaymentCards()).hasSize(3);
    }

    @Test
    void updateActiveStatusShouldUpdateUser() {
        int rows = repository.updateActiveStatus(1L, Boolean.FALSE);

        User user = repository.findById(1L)
            .orElseThrow();
        assertThat(rows).isEqualTo(1);
        assertThat(user.getActive()).isFalse();
    }

    @Test
    void updateActiveStatusShouldReturnZeroWhenUserNotExist() {
        int rows = repository.updateActiveStatus(999L, Boolean.FALSE);

        assertThat(rows).isZero();
    }

    @Test
    void existsByEmailShouldReturnTrueWhenEmailExists() {
        boolean exists = repository.existsByEmail("raddanprofile@gmail.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailShouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = repository.existsByEmail("unknown@mail.com");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailAndIdNotShouldReturnTrueWhenEmailExistsForAnotherUser() {
        boolean exists = repository.existsByEmailAndIdNot(
            "raddanprofile@gmail.com",
            2L
        );

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailAndIdNotShouldReturnFalseWhenEmailBelongsToSameUser() {
        boolean exists = repository.existsByEmailAndIdNot(
            "raddanprofile@gmail.com",
            1L
        );

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailAndIdNotShouldReturnFalseWhenEmailDoesNotExist() {
        boolean exists = repository.existsByEmailAndIdNot(
            "unknown@mail.com",
            1L
        );

        assertThat(exists).isFalse();
    }

    @Test
    void findByIdForUpdateShouldReturnUserWithCards() {
        User user = repository.findByIdForUpdate(1L)
            .orElseThrow();

        assertThat(user.getName()).isEqualTo("Alexander");
        assertThat(user.getSurname()).isEqualTo("Dudkin");
        assertThat(user.getPaymentCards()).hasSize(3);
    }

    @Test
    void findByIdForUpdateShouldReturnEmptyWhenUserDoesNotExist() {
        Optional<User> user = repository.findByIdForUpdate(999L);

        assertThat(user).isEmpty();
    }

}
