package org.example.ecommerce.auth.service.registration;

import org.example.ecommerce.auth.entity.RegistrationCompensationOutbox;
import org.example.ecommerce.auth.repository.RegistrationCompensationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RegistrationCompensationIncidentServiceTests {

    @Mock
    private RegistrationCompensationOutboxRepository outboxRepository;

    @InjectMocks
    private RegistrationCompensationIncidentService incidentService;

    @Test
    void saveShouldPersistOutboxIncident() {
        RuntimeException originalException = new RuntimeException("original failure");
        RuntimeException compensationException = new RuntimeException("compensation failure");

        incidentService.save(101L, originalException, compensationException);

        ArgumentCaptor<RegistrationCompensationOutbox> captor =
            ArgumentCaptor.forClass(RegistrationCompensationOutbox.class);

        verify(outboxRepository).saveAndFlush(captor.capture());

        RegistrationCompensationOutbox incident = captor.getValue();

        assertEquals(101L, incident.getUserId());
        assertEquals(0, incident.getAttemptCount());
        assertEquals(org.example.ecommerce.auth.enums.CompensationStatus.PENDING, incident.getStatus());

        String lastError = (String) ReflectionTestUtils.getField(incident, "lastError");
        assert lastError != null;
        assertTrue(lastError.contains("Original error: RuntimeException: original failure"));
        assertTrue(lastError.contains("Compensation error: RuntimeException: compensation failure"));
    }
}
