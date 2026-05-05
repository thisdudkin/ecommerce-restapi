package org.example.ecommerce.payments.infrastructure.external.client;

import org.example.ecommerce.payments.infrastructure.external.dto.RandomOrgRequest;
import org.example.ecommerce.payments.infrastructure.external.dto.RandomOrgResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "random-org",
    url = "${clients.random-org.base-url}"
)
public interface RandomOrgClient {

    @PostMapping(
        value = "/json-rpc/4/invoke",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    RandomOrgResponse invoke(@RequestBody RandomOrgRequest request);

}
