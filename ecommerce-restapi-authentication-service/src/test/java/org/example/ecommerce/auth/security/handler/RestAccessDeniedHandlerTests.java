package org.example.ecommerce.auth.security.handler;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestAccessDeniedHandlerTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void handleShouldWriteForbiddenProblemResponse() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
            request,
            response,
            new AccessDeniedException("Access denied")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertEquals(403, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("Access denied", body.get("title").asString());
        assertEquals("You do not have permission to access this resource", body.get("detail").asString());
        assertEquals(403, body.get("status").asInt());
        assertEquals("/api/v1/admin", body.get("instance").asString());
    }

}
