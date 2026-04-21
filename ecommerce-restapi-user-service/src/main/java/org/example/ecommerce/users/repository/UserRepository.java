package org.example.ecommerce.users.repository;

import jakarta.persistence.LockModeType;
import org.example.ecommerce.users.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.LOAD;

public interface UserRepository extends JpaRepository<User, Long>,
    JpaSpecificationExecutor<User>, org.example.ecommerce.users.repository.pagination.UserRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(value = "userWithCards", type = LOAD)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(Long id);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    @EntityGraph(value = "userWithCards", type = LOAD)
    Optional<User> findByIdWithCards(Long id);

    @Modifying
    @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
    int updateActiveStatus(Long id, Boolean active);

    @EntityGraph(value = "userWithCards", type = LOAD)
    List<User> findAllByIdIn(Collection<Long> ids);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByIdAndActiveTrue(Long userId);

}
