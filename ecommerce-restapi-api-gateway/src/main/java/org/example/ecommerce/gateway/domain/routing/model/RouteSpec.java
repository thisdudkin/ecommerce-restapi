package org.example.ecommerce.gateway.domain.routing.model;

import java.util.List;

public record RouteSpec(
    String id,
    List<String> paths,
    String uri,
    List<String> headersToRemove
) { }
