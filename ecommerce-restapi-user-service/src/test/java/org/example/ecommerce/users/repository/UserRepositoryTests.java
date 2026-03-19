package org.example.ecommerce.users.repository;

import org.example.ecommerce.users.entity.User;
import org.example.ecommerce.users.repository.enums.SortDirection;
import org.example.ecommerce.users.repository.specifications.UserSpecifications;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
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
    void findWindowShouldReturnFirstWindowUsingKeysetPagination() {
        Window<User> window = repository.findWindow(
            UserSpecifications.withFilters(null, null),
            PageRequest.of(0, 2, UserRepository.keysetSort(SortDirection.DESC)),
            ScrollPosition.keyset()
        );

        List<User> users = window.getContent();
        assertThat(users).hasSize(2);
        assertThat(users)
            .extracting(User::getSurname)
            .containsExactly("Nabok", "Kolesneva");
        assertThat(window.hasNext()).isTrue();
    }

    @Test
    void findWindowShouldFilterByNameIgnoringCaseAndTrim() {
        Window<User> window = repository.findWindow(
            UserSpecifications.withFilters("  alex  ", null),
            PageRequest.of(0, 10, UserRepository.keysetSort(SortDirection.ASC)),
            ScrollPosition.keyset()
        );

        List<User> users = window.getContent();
        assertThat(users).isNotEmpty();
        assertThat(users)
            .extracting(User::getName)
            .allMatch(name -> name.toLowerCase().contains("alex"));
    }

    @Test
    void findWindowShouldFilterBySurnameIgnoringCaseAndTrim() {
        Window<User> window = repository.findWindow(
            UserSpecifications.withFilters(null, "  dud  "),
            PageRequest.of(0, 10, UserRepository.keysetSort(SortDirection.ASC)),
            ScrollPosition.keyset()
        );

        List<User> users = window.getContent();
        assertThat(users).isNotEmpty();
        assertThat(users)
            .extracting(User::getSurname)
            .allMatch(surname -> surname.toLowerCase().contains("dud"));
    }

    @Test
    void findWindowShouldFilterByNameAndSurname() {
        Window<User> window = repository.findWindow(
            UserSpecifications.withFilters("alex", "dud"),
            PageRequest.of(0, 10, UserRepository.keysetSort(SortDirection.ASC)),
            ScrollPosition.keyset()
        );

        List<User> users = window.getContent();
        assertThat(users).hasSize(1);
        assertThat(users.getFirst().getName()).isEqualTo("Alexander");
        assertThat(users.getFirst().getSurname()).isEqualTo("Dudkin");
    }

    @Test
    void findWindowShouldIgnoreBlankFilters() {
        Window<User> window = repository.findWindow(
            UserSpecifications.withFilters("   ", "   "),
            PageRequest.of(0, 10, UserRepository.keysetSort(SortDirection.ASC)),
            ScrollPosition.keyset()
        );

        List<User> users = window.getContent();
        assertThat(users).isNotEmpty();
    }

    @Test
    void findWindowShouldReturnSecondWindowUsingKeysetPagination() {
        Window<User> firstWindow = repository.findWindow(
            UserSpecifications.withFilters(null, null),
            PageRequest.of(0, 2, UserRepository.keysetSort(SortDirection.DESC)),
            ScrollPosition.keyset()
        );

        ScrollPosition nextPosition = firstWindow.positionAt(firstWindow.size() - 1);

        Window<User> secondWindow = repository.findWindow(
            UserSpecifications.withFilters(null, null),
            PageRequest.of(0, 2, UserRepository.keysetSort(SortDirection.DESC)),
            nextPosition
        );

        List<User> users = secondWindow.getContent();

        assertThat(users).hasSize(2);
        assertThat(users)
            .extracting(User::getSurname)
            .containsExactly("Nechay-Nicevich", "Inchakov");
        assertThat(secondWindow.hasNext()).isTrue();
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
