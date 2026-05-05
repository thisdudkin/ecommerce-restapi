package org.example.ecommerce.auth.repository;

import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.security.enums.Role;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@Testcontainers
@ActiveProfiles("test")
@DataJpaTest(showSql = false)
@Sql("classpath:insert-auth.sql")
@AutoConfigureTestDatabase(replace = NONE)
class UserCredentialRepositoryIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.0");

    @Autowired
    private UserCredentialRepository repository;

    @Test
    void findByLoginShouldReturnCredential() {
        UserCredential credential = repository.findByLogin("alex.user")
            .orElseThrow();

        assertThat(credential.getUserId()).isEqualTo(101L);
        assertThat(credential.getLogin()).isEqualTo("alex.user");
        assertThat(credential.getRole()).isEqualTo(Role.USER);
        assertThat(credential.getActive()).isTrue();
    }

    @Test
    void findByLoginShouldReturnEmptyWhenCredentialDoesNotExist() {
        Optional<UserCredential> credential = repository.findByLogin("missing.user");

        assertThat(credential).isEmpty();
    }

    @Test
    void findByUserIdShouldReturnCredential() {
        UserCredential credential = repository.findByUserId(103L)
            .orElseThrow();

        assertThat(credential.getLogin()).isEqualTo("admin.user");
        assertThat(credential.getRole()).isEqualTo(Role.ADMIN);
        assertThat(credential.getActive()).isTrue();
    }

    @Test
    void existsByLoginShouldReturnTrueWhenCredentialExists() {
        boolean exists = repository.existsByLogin("inactive.user");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByLoginShouldReturnFalseWhenCredentialDoesNotExist() {
        boolean exists = repository.existsByLogin("unknown.user");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByUserIdShouldReturnTrueWhenCredentialExists() {
        boolean exists = repository.existsByUserId(101L);

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUserIdShouldReturnFalseWhenCredentialDoesNotExist() {
        boolean exists = repository.existsByUserId(999L);

        assertThat(exists).isFalse();
    }

}
