package org.example.ecommerce.auth.repository;

import org.example.ecommerce.auth.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

    Optional<UserCredential> findByLogin(String login);

    Optional<UserCredential> findByUserId(Long userId);

    boolean existsByLogin(String login);

    boolean existsByUserId(Long userId);

}
