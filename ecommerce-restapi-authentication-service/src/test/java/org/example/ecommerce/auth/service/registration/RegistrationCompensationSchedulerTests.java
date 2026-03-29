package org.example.ecommerce.auth.service.registration;

import org.example.ecommerce.auth.entity.RegistrationCompensationOutbox;
import org.example.ecommerce.auth.enums.CompensationStatus;
import org.example.ecommerce.auth.repository.RegistrationCompensationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationCompensationSchedulerTests {

    @Mock
    private RegistrationCompensationOutboxRepository outboxRepository;

    @Mock
    private RegistrationCompensationRetryService retryService;

    @InjectMocks
    private RegistrationCompensationScheduler scheduler;

    @Test
    void processShouldPollIncidentsAndRetryEachOne() {
        RegistrationCompensationOutbox first = incident(1L, 101L);
        RegistrationCompensationOutbox second = incident(2L, 102L);

        when(outboxRepository.poll(any(CompensationStatus.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(List.of(first, second));

        scheduler.process();

        verify(outboxRepository).poll(any(CompensationStatus.class), any(LocalDateTime.class), any(Pageable.class));
        verify(retryService).retry(1L);
        verify(retryService).retry(2L);
    }

    private RegistrationCompensationOutbox incident(Long id, Long userId) {
        RegistrationCompensationOutbox incident =
            new RegistrationCompensationOutbox(userId, "error");

        ReflectionTestUtils.setField(incident, "id", id);
        return incident;
    }
}
