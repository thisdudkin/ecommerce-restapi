package org.example.ecommerce.auth.service.registration;

import feign.FeignException.NotFound;
import org.example.ecommerce.auth.client.UserClient;
import org.example.ecommerce.auth.exception.custom.CompensationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RegistrationCompensationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationCompensationService.class);

    private final UserClient userClient;
    private final RegistrationCompensationIncidentService incidentService;

    public RegistrationCompensationService(UserClient userClient,
                                           RegistrationCompensationIncidentService incidentService) {
        this.userClient = userClient;
        this.incidentService = incidentService;
    }

    public void rollbackUserCreation(Long userId, RuntimeException originalException) {
        try {
            userClient.delete(userId);
            log.info("Compensation succeeded. Deleted userId='{}' from user-service", userId);

        } catch (NotFound e) {
            log.info("Compensation delete returned 404 for userId='{}'. Treating as successful rollback", userId);
        } catch (Exception compensationException) {
            log.error("Failed to rollback user creation for userId='{}'", userId, compensationException);

            incidentService.save(userId, originalException, compensationException);

            CompensationFailedException exception = new CompensationFailedException(
                "Registration failed and cleanup of created user could not be completed immediately for userId='%s'"
                    .formatted(userId),
                originalException
            );
            exception.addSuppressed(compensationException);

            throw exception;
        }
    }

}
