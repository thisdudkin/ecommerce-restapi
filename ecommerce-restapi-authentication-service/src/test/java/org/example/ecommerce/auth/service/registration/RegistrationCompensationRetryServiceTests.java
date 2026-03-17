package org.example.ecommerce.auth.service.registration;

import feign.FeignException;
import feign.FeignException.NotFound;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.example.ecommerce.auth.client.UserClient;
import org.example.ecommerce.auth.entity.RegistrationCompensationOutbox;
import org.example.ecommerce.auth.enums.CompensationStatus;
import org.example.ecommerce.auth.repository.RegistrationCompensationOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationCompensationRetryServiceTests {

    @Mock
    private RegistrationCompensationOutboxRepository outboxRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private RegistrationCompensationRetryService retryService;

    @Test
    void retryShouldThrowWhenIncidentDoesNotExist() {
        when(outboxRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> retryService.retry(1L));
    }

    @Test
    void retryShouldReturnWhenIncidentIsNotPending() {
        RegistrationCompensationOutbox incident = incident(
            CompensationStatus.COMPLETED,
            0,
            LocalDateTime.now().minusMinutes(1)
        );

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(incident));

        retryService.retry(1L);

        verify(outboxRepository).findById(1L);
        verifyNoInteractions(userClient);
    }

    @Test
    void retryShouldReturnWhenNextRetryAtIsInFuture() {
        RegistrationCompensationOutbox incident = incident(
            CompensationStatus.PENDING,
            0,
            LocalDateTime.now().plusMinutes(5)
        );

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(incident));

        retryService.retry(1L);

        verify(outboxRepository).findById(1L);
        verifyNoInteractions(userClient);
    }

    @Test
    void retryShouldMarkCompletedWhenDeleteSucceeds() {
        RegistrationCompensationOutbox incident = incident(
            CompensationStatus.PENDING,
            0,
            LocalDateTime.now().minusMinutes(1)
        );

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(incident));

        retryService.retry(1L);

        assertEquals(CompensationStatus.COMPLETED, incident.getStatus());
        assertEquals(1, incident.getAttemptCount());
        assertNotNull(incident.getNextRetryAt());
        assertNull(ReflectionTestUtils.getField(incident, "lastError"));

        verify(userClient).delete(101L);
    }

    @Test
    void retryShouldMarkCompletedWhenDeleteReturns404() {
        RegistrationCompensationOutbox incident = incident(
            CompensationStatus.PENDING,
            0,
            LocalDateTime.now().minusMinutes(1)
        );

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(incident));
        doThrow(notFound()).when(userClient).delete(101L);

        retryService.retry(1L);

        assertEquals(CompensationStatus.COMPLETED, incident.getStatus());
        assertEquals(1, incident.getAttemptCount());
        verify(userClient).delete(101L);
    }

    @Test
    void retryShouldKeepPendingWhenRetryFailsBeforeMaxAttempts() {
        RegistrationCompensationOutbox incident = incident(
            CompensationStatus.PENDING,
            0,
            LocalDateTime.now().minusMinutes(1)
        );

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(incident));
        doThrow(new RuntimeException("delete failed")).when(userClient).delete(101L);

        retryService.retry(1L);

        assertEquals(CompensationStatus.PENDING, incident.getStatus());
        assertEquals(1, incident.getAttemptCount());
        assertTrue(incident.getNextRetryAt().isAfter(LocalDateTime.now().minusSeconds(1)));

        String lastError = (String) ReflectionTestUtils.getField(incident, "lastError");
        assert lastError != null;
        assertTrue(lastError.contains("RuntimeException: delete failed"));
    }

    @Test
    void retryShouldMarkDeadWhenMaxAttemptsReached() {
        RegistrationCompensationOutbox incident = incident(
            CompensationStatus.PENDING,
            9,
            LocalDateTime.now().minusMinutes(1)
        );

        when(outboxRepository.findById(1L)).thenReturn(Optional.of(incident));
        doThrow(new RuntimeException("delete failed")).when(userClient).delete(101L);

        retryService.retry(1L);

        assertEquals(CompensationStatus.DEAD, incident.getStatus());
        assertEquals(10, incident.getAttemptCount());
    }

    private RegistrationCompensationOutbox incident(CompensationStatus status,
                                                    int attemptCount,
                                                    LocalDateTime nextRetryAt) {
        RegistrationCompensationOutbox incident =
            new RegistrationCompensationOutbox(101L, "last error");

        ReflectionTestUtils.setField(incident, "id", 1L);
        ReflectionTestUtils.setField(incident, "status", status);
        ReflectionTestUtils.setField(incident, "attemptCount", attemptCount);
        ReflectionTestUtils.setField(incident, "nextRetryAt", nextRetryAt);

        return incident;
    }

    private NotFound notFound() {
        Request request = Request.create(
            Request.HttpMethod.DELETE,
            "http://localhost/api/v1/users/101",
            Map.of(),
            null,
            StandardCharsets.UTF_8,
            new RequestTemplate()
        );

        Response response = Response.builder()
            .status(404)
            .reason("Not Found")
            .request(request)
            .headers(Map.of())
            .build();

        return (NotFound) FeignException.errorStatus("UserClient#delete(Long)", response);
    }
}
