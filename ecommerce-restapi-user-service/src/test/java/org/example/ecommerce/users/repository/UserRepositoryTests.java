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
        // Act
        User user = repository.findByIdWithCards(1L)
            .orElseThrow();

        // Assert
        assertThat(user.getName()).isEqualTo("Alexander");
        assertThat(user.getSurname()).isEqualTo("Dudkin");
        assertThat(user.getPaymentsCards()).hasSize(3);
    }

    @Test
    void findByIdWithCardsShouldReturnEmptyWhenUserDoesNotExist() {
        // Act
        Optional<User> undefined = repository.findByIdWithCards(999L);

        // Assert
        assertThat(undefined).isEmpty();
    }

    @Test
    void findWindowShouldReturnFirstWindowUsingKeysetPagination() {
        // Act
        Window<User> window = repository.findWindow(
            UserSpecifications.withFilters(null, null),
            PageRequest.of(0, 2, UserRepository.keysetSort(SortDirection.DESC)),
            ScrollPosition.keyset()
        );

        // Assert
        List<User> users = window.getContent();
        assertThat(users).hasSize(2);
        assertThat(users)
            .extracting(User::getSurname)
            .containsExactly("Nabok", "Kolesneva");
        assertThat(window.hasNext()).isTrue();
    }

    @Test
    void findWindowShouldReturnSecondWindowUsingKeysetPagination() {
        // Act
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

        // Assert
        List<User> users = secondWindow.getContent();

        assertThat(users).hasSize(2);
        assertThat(users)
            .extracting(User::getSurname)
            .containsExactly("Nechay-Nicevich", "Inchakov");
        assertThat(secondWindow.hasNext()).isTrue();
    }

    @Test
    void updateActiveStatusShouldUpdateUser() {
        // Act
        int rows = repository.updateActiveStatus(1L, Boolean.FALSE);

        // Assert
        User user = repository.findById(1L)
            .orElseThrow();
        assertThat(rows).isEqualTo(1);
        assertThat(user.getActive()).isFalse();
    }

    @Test
    void updateActiveStatusShouldReturnZeroWhenUserNotExist() {
        // Act
        int rows = repository.updateActiveStatus(999L, Boolean.FALSE);

        // Assert
        assertThat(rows).isZero();
    }

    @Test
    void existsByEmailShouldReturnTrueWhenEmailExists() {
        // Act
        boolean exists = repository.existsByEmail("raddanprofile@gmail.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailShouldReturnFalseWhenEmailDoesNotExist() {
        // Act
        boolean exists = repository.existsByEmail("unknown@mail.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailAndIdNotShouldReturnTrueWhenEmailExistsForAnotherUser() {
        // Act
        boolean exists = repository.existsByEmailAndIdNot(
            "raddanprofile@gmail.com",
            2L
        );

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmailAndIdNotShouldReturnFalseWhenEmailBelongsToSameUser() {
        // Act
        boolean exists = repository.existsByEmailAndIdNot(
            "raddanprofile@gmail.com",
            1L
        );

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailAndIdNotShouldReturnFalseWhenEmailDoesNotExist() {
        // Act
        boolean exists = repository.existsByEmailAndIdNot(
            "unknown@mail.com",
            1L
        );

        // Assert
        assertThat(exists).isFalse();
    }

}
