package org.example.ecommerce.auth.security.handler;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestAuthenticationEntryPointTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void commenceShouldWriteUnauthorizedProblemResponse() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/protected");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
            request,
            response,
            new InsufficientAuthenticationException("Authentication required")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("Authentication required", body.get("title").asString());
        assertEquals("Authentication is required to access this resource", body.get("detail").asString());
        assertEquals("AUTHENTICATION_REQUIRED", body.get("errorCode").asString());
        assertEquals("/api/v1/protected", body.get("path").asString());
    }
}
