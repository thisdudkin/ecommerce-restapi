package org.example.ecommerce.users.repository;

import org.example.ecommerce.users.entity.PaymentCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PaymentCardRepository extends JpaRepository<PaymentCard, Long>,
    JpaSpecificationExecutor<PaymentCard> {

    @EntityGraph(attributePaths = "user")
    @Query("SELECT c FROM PaymentCard c WHERE c.id = :id")
    Optional<PaymentCard> findByIdWithUser(Long id);

    @Query(value = """
        SELECT id, user_id, number, holder, expiration_date,
               active, created_at, updated_at
        FROM payments_cards
        WHERE user_id = :userId
        ORDER BY created_at, id DESC
        """, nativeQuery = true)
    List<PaymentCard> findAllByUserId(Long userId);

    @Query("SELECT number FROM PaymentCard WHERE number IN :numbers")
    Set<String> findExistingNumbers(Collection<String> numbers);

    long countByUserId(Long userId);

    boolean existsByNumber(String number);

    @Modifying
    @Query("UPDATE PaymentCard c SET c.active = :active WHERE c.id = :id")
    int updateActiveStatus(Long id, Boolean active);

}
