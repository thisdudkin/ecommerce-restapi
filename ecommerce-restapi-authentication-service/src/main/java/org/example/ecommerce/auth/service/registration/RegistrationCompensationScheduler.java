package org.example.ecommerce.auth.service.registration;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.example.ecommerce.auth.entity.RegistrationCompensationOutbox;
import org.example.ecommerce.auth.enums.CompensationStatus;
import org.example.ecommerce.auth.repository.RegistrationCompensationOutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RegistrationCompensationScheduler {

    private static final int BATCH_SIZE = 20;

    private final RegistrationCompensationOutboxRepository outboxRepository;
    private final RegistrationCompensationRetryService retryService;

    public RegistrationCompensationScheduler(RegistrationCompensationOutboxRepository outboxRepository,
                                             RegistrationCompensationRetryService retryService) {
        this.outboxRepository = outboxRepository;
        this.retryService = retryService;
    }

    @Scheduled(fixedDelayString = "${registration.compensation.fixed-delay-ms:30000}")
    @SchedulerLock(
        name = "registrationCompensationScheduler",
        lockAtLeastFor = "PT5S",
        lockAtMostFor = "PT1M"
    )
    public void process() {
        List<Long> incidentIds = outboxRepository.poll(
                CompensationStatus.PENDING,
                LocalDateTime.now(),
                PageRequest.of(0, BATCH_SIZE)).stream()
            .map(RegistrationCompensationOutbox::getId)
            .toList();

        incidentIds.forEach(retryService::retry);
    }

}
