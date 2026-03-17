package org.example.ecommerce.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.example.ecommerce.auth.enums.CompensationStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.springframework.util.StringUtils.truncate;

@Entity
@Table(
    name = "registration_compensation_outbox",
    indexes = {
        @Index(
            name = "idx_registration_compensation_outbox_status_next_retry",
            columnList = "status, next_retry_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "registration_compensation_outbox_user_id_key",
            columnNames = {"user_id"}
        )
    }
)
public class RegistrationCompensationOutbox {

    private static final int MAX_ERROR_LENGTH = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private CompensationStatus status = CompensationStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt = LocalDateTime.now();

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RegistrationCompensationOutbox() {
    }

    public RegistrationCompensationOutbox(Long userId, String lastError) {
        this.userId = userId;
        this.attemptCount = 0;
        this.nextRetryAt = LocalDateTime.now();
        this.lastError = truncate(lastError, MAX_ERROR_LENGTH);
    }

    public void markCompleted() {
        this.status = CompensationStatus.COMPLETED;
        this.attemptCount++;
        this.lastError = null;
        this.nextRetryAt = LocalDateTime.now();
    }

    public void markRetryFailed(String errorMessage, Duration delay, int maxAttempts) {
        this.attemptCount++;
        this.lastError = truncate(errorMessage, MAX_ERROR_LENGTH);

        if (this.attemptCount >= maxAttempts) {
            this.status = CompensationStatus.DEAD;
            this.nextRetryAt = LocalDateTime.now();
            return;
        }

        this.status = CompensationStatus.PENDING;
        this.nextRetryAt = LocalDateTime.now().plus(delay);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public CompensationStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

}
