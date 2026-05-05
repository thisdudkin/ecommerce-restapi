package org.example.ecommerce.payments.infrastructure.external.dto;

public record RandomOrgRequest(
    String jsonrpc,
    String method,
    Params params,
    long id
) {
    public static RandomOrgRequest singleNumber(String apiKey, int min, int max, long id) {
        return new RandomOrgRequest(
            "2.0",
            "generateIntegers",
            new Params(
                apiKey,
                1,
                min,
                max
            ),
            id
        );
    }

    public record Params(
        String apiKey,
        int n,
        int min,
        int max
    ) {}
}
