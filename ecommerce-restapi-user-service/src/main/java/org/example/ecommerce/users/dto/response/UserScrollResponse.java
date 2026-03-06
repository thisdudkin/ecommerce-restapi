package org.example.ecommerce.users.dto.response;

import java.io.Serializable;
import java.util.List;

public record UserScrollResponse(
    List<UserListResponse> items,
    boolean hasNext,
    String nextCursor
) implements Serializable { }
