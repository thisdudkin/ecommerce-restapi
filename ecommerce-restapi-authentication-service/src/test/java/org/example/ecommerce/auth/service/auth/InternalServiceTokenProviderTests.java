package org.example.ecommerce.auth.service.auth;

import org.example.ecommerce.auth.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalServiceTokenProviderTests {

    @Mock
    private JwtService jwtService;

    @Test
    void getTokenShouldDelegateToJwtService() {
        InternalServiceTokenProvider provider =
            new InternalServiceTokenProvider(jwtService, "authentication-service");

        when(jwtService.generateInternalServiceToken("authentication-service"))
            .thenReturn("internal-token");

        String actual = provider.getToken();

        assertEquals("internal-token", actual);
        verify(jwtService).generateInternalServiceToken("authentication-service");
    }
}
