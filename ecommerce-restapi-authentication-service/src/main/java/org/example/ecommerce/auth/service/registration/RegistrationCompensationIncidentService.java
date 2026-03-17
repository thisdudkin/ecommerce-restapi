package org.example.ecommerce.auth.service.registration;

import org.example.ecommerce.auth.entity.RegistrationCompensationOutbox;
import org.example.ecommerce.auth.repository.RegistrationCompensationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationCompensationIncidentService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationCompensationIncidentService.class);

    private final RegistrationCompensationOutboxRepository outboxRepository;

    public RegistrationCompensationIncidentService(RegistrationCompensationOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(Long userId, Throwable originalException, Throwable compensationException) {
        String payload = """
            Original error: %s: %s
            Compensation error: %s: %s
            """.formatted(
            originalException.getClass().getSimpleName(), originalException.getMessage(),
            compensationException.getClass().getSimpleName(), compensationException.getMessage()
        );

        RegistrationCompensationOutbox incident = new RegistrationCompensationOutbox(userId, payload);

        outboxRepository.saveAndFlush(incident);

        log.error(
            "Persisted failed compensation incident, userId = '{}', incidentId = '{}'",
            userId,
            incident.getId()
        );
    }

}
