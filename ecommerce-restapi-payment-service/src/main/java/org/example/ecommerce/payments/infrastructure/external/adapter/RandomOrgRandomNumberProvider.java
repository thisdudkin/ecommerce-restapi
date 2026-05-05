package org.example.ecommerce.payments.infrastructure.external.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ecommerce.payments.config.RandomOrgClientProperties;
import org.example.ecommerce.payments.infrastructure.exception.RandomNumberGenerationException;
import org.example.ecommerce.payments.infrastructure.external.client.RandomOrgClient;
import org.example.ecommerce.payments.infrastructure.external.dto.RandomOrgRequest;
import org.example.ecommerce.payments.infrastructure.external.dto.RandomOrgResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class RandomOrgRandomNumberProvider implements RandomNumberProvider {

    private final RandomOrgClient client;
    private final RandomOrgClientProperties properties;

    @Override
    public int nextInt() {
        RandomOrgRequest request = buildRequest();

        try {
            RandomOrgResponse response = client.invoke(request);
            return extractNumber(response);
        } catch (RandomNumberGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get random number from random.org", e);
            throw new RandomNumberGenerationException("Failed to get random number from random.org", e);
        }
    }

    private RandomOrgRequest buildRequest() {
        return RandomOrgRequest.singleNumber(
            properties.apiKey(),
            ThreadLocalRandom.current().nextInt(1, 10),
            ThreadLocalRandom.current().nextInt(11, 101),
            ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE)
        );
    }

    private int extractNumber(RandomOrgResponse response) {
        if (response == null) {
            throw new RandomNumberGenerationException("Random.org response is null");
        }

        if (response.error() != null) {
            throw new RandomNumberGenerationException("Random.org error: Code: %s, Message: %s"
                .formatted(response.error().code(), response.error().message())
            );
        }

        if (response.result() == null
            || response.result().random() == null
            || response.result().random().data() == null
            || response.result().random().data().isEmpty()) {
            throw new RandomNumberGenerationException("Random.org returned empty random data");
        }

        return response.result().random().data().getFirst();
    }

}
