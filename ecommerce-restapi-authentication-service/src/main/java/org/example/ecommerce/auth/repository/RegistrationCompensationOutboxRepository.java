package org.example.ecommerce.auth.repository;

import org.example.ecommerce.auth.entity.RegistrationCompensationOutbox;
import org.example.ecommerce.auth.enums.CompensationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RegistrationCompensationOutboxRepository extends JpaRepository<RegistrationCompensationOutbox, Long> {

    @Query(value = """
        SELECT id, user_id, status, attempt_count, next_retry_at,
               last_error, created_at, updated_at
        FROM registration_compensation_outbox
        WHERE status = :#{#status.name()}
          AND next_retry_at <= :nextRetryAt
        ORDER BY created_at
        """, nativeQuery = true)
    List<RegistrationCompensationOutbox> poll(CompensationStatus status,
                                              LocalDateTime nextRetryAt,
                                              Pageable pageable);

}
