package org.example.ecommerce.auth.service.registration;

import feign.FeignException;
import feign.FeignException.NotFound;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.example.ecommerce.auth.client.UserClient;
import org.example.ecommerce.auth.exception.custom.CompensationFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationCompensationServiceTests {

    @Mock
    private UserClient userClient;

    @Mock
    private RegistrationCompensationIncidentService incidentService;

    @InjectMocks
    private RegistrationCompensationService compensationService;

    @Test
    void rollbackUserCreationShouldDeleteUserWhenCompensationSucceeds() {
        RuntimeException originalException = new RuntimeException("original failure");

        compensationService.rollbackUserCreation(101L, originalException);

        verify(userClient).delete(101L);
        verifyNoInteractions(incidentService);
    }

    @Test
    void rollbackUserCreationShouldTreat404AsSuccess() {
        RuntimeException originalException = new RuntimeException("original failure");

        doThrow(notFound()).when(userClient).delete(101L);

        compensationService.rollbackUserCreation(101L, originalException);

        verify(userClient).delete(101L);
        verifyNoInteractions(incidentService);
    }

    @Test
    void rollbackUserCreationShouldSaveIncidentAndThrowWhenCompensationFails() {
        RuntimeException originalException = new RuntimeException("original failure");
        RuntimeException compensationException = new RuntimeException("delete failed");

        doThrow(compensationException).when(userClient).delete(101L);

        CompensationFailedException actual = assertThrows(
            CompensationFailedException.class,
            () -> compensationService.rollbackUserCreation(101L, originalException)
        );

        verify(userClient).delete(101L);
        verify(incidentService).save(101L, originalException, compensationException);
        assertEquals(1, actual.getSuppressed().length);
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
