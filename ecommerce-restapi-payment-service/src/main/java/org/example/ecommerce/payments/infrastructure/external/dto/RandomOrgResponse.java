package org.example.ecommerce.payments.infrastructure.external.dto;

import java.util.List;

public record RandomOrgResponse(
    String jsonrpc,
    Result result,
    Error error,
    Long id
) {
    public record Result(
        Random random,
        Integer bitsUsed,
        Integer bitsLeft,
        Integer requestsLeft,
        Integer advisoryDelay
    ) { }

    public record Random(
        List<Integer> data,
        String completionTime
    ) { }

    public record Error(
        Integer code,
        String message,
        Object data
    ) { }
}
