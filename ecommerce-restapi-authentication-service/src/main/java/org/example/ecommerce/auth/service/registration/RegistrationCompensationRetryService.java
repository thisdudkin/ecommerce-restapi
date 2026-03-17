package org.example.ecommerce.auth.service.registration;

import feign.FeignException.NotFound;
import org.example.ecommerce.auth.client.UserClient;
import org.example.ecommerce.auth.entity.RegistrationCompensationOutbox;
import org.example.ecommerce.auth.enums.CompensationStatus;
import org.example.ecommerce.auth.repository.RegistrationCompensationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class RegistrationCompensationRetryService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationCompensationRetryService.class);
    private static final int MAX_ATTEMPTS = 10;

    private final RegistrationCompensationOutboxRepository outboxRepository;
    private final UserClient userClient;

    public RegistrationCompensationRetryService(RegistrationCompensationOutboxRepository outboxRepository, UserClient userClient) {
        this.outboxRepository = outboxRepository;
        this.userClient = userClient;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retry(Long incidentId) {
        RegistrationCompensationOutbox incident = outboxRepository.findById(incidentId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Compensation incident '%s' not found".formatted(incidentId)
            ));

        if (incident.getStatus() != CompensationStatus.PENDING ||
            incident.getNextRetryAt().isAfter(LocalDateTime.now()))
            return;

        try {
            userClient.delete(incident.getUserId());
            incident.markCompleted();

            log.info(
                "Compensation retry succeeded. incidentId='{}', userId='{}'",
                incident.getId(),
                incident.getUserId()
            );
        } catch (NotFound e) {
            incident.markCompleted();

            log.info(
                "Compensation retry got 404 and is treated as success. incidentId='{}', userId='{}'",
                incident.getId(),
                incident.getUserId()
            );
        } catch (Exception e) {
            Duration delay = calculateBackoff(incident.getAttemptCount() + 1);
            incident.markRetryFailed(buildErrorMessage(e), delay, MAX_ATTEMPTS);

            if (incident.getStatus() == CompensationStatus.DEAD) {
                log.error(
                    "Compensation retry exhausted. incidentId='{}', userId='{}'",
                    incident.getId(),
                    incident.getUserId(),
                    e
                );
            } else {
                log.warn(
                    "Compensation retry failed. incidentId='{}', userId='{}'. Will retry later",
                    incident.getId(),
                    incident.getUserId(),
                    e
                );
            }
        }
    }

    private Duration calculateBackoff(int attemptNumber) {
        return switch (attemptNumber) {
            case 1 -> Duration.ofSeconds(30);
            case 2 -> Duration.ofMinutes(1);
            case 3 -> Duration.ofMinutes(5);
            default -> Duration.ofMinutes(15);
        };
    }

    private String buildErrorMessage(Throwable e) {
        return "%s: %s".formatted(e.getClass().getSimpleName(), e.getMessage());
    }

}
