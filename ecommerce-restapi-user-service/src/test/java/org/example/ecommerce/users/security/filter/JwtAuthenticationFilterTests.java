package org.example.ecommerce.users.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.example.ecommerce.users.exception.custom.InvalidJwtException;
import org.example.ecommerce.users.security.enums.JwtType;
import org.example.ecommerce.users.security.jwt.JwtService;
import org.example.ecommerce.users.security.jwt.VerifiedJwtClaims;
import org.example.ecommerce.users.security.principal.UserSecurityPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternalShouldContinueWhenAuthorizationHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertTrue(chain.invoked);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternalShouldContinueWhenAuthorizationHeaderIsNotBearer() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        filter.doFilter(request, response, chain);

        assertTrue(chain.invoked);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternalShouldAuthenticateInternalService() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer internal-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        VerifiedJwtClaims claims = new VerifiedJwtClaims(
            null,
            "authentication-service",
            null,
            JwtType.INTERNAL,
            true,
            "authentication-service",
            null,
            null
        );

        when(jwtService.parse("internal-token")).thenReturn(claims);
        when(jwtService.isTrustedInternalService(claims)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertTrue(chain.invoked);
        assertNotNull(authentication);
        assertInstanceOf(UserSecurityPrincipal.class, authentication.getPrincipal());
        assertEquals("ROLE_INTERNAL_SERVICE", authentication.getAuthorities().iterator().next().getAuthority());

        UserSecurityPrincipal principal = (UserSecurityPrincipal) authentication.getPrincipal();
        assertNull(principal.userId());
        assertEquals("authentication-service", principal.subject());
        assertTrue(principal.internal());

        verify(jwtService).parse("internal-token");
        verify(jwtService).isTrustedInternalService(claims);
    }

    @Test
    void doFilterInternalShouldAuthenticateUserAccessToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer user-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        VerifiedJwtClaims claims = new VerifiedJwtClaims(
            101L,
            "alex.user",
            "ADMIN",
            JwtType.ACCESS,
            false,
            null,
            null,
            null
        );

        when(jwtService.parse("user-token")).thenReturn(claims);
        when(jwtService.isTrustedInternalService(claims)).thenReturn(false);
        when(jwtService.isUserAccessToken(claims)).thenReturn(true);

        filter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertTrue(chain.invoked);
        assertNotNull(authentication);
        assertInstanceOf(UserSecurityPrincipal.class, authentication.getPrincipal());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());

        UserSecurityPrincipal principal = (UserSecurityPrincipal) authentication.getPrincipal();
        assertEquals(101L, principal.userId());
        assertEquals("alex.user", principal.subject());
        assertFalse(principal.internal());

        verify(jwtService).parse("user-token");
        verify(jwtService).isTrustedInternalService(claims);
        verify(jwtService).isUserAccessToken(claims);
    }

    @Test
    void doFilterInternalShouldReturnUnauthorizedWhenTokenTypeIsUnsupported() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer unsupported-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        VerifiedJwtClaims claims = new VerifiedJwtClaims(
            101L,
            "alex.user",
            "USER",
            JwtType.REFRESH,
            false,
            null,
            null,
            null
        );

        when(jwtService.parse("unsupported-token")).thenReturn(claims);
        when(jwtService.isTrustedInternalService(claims)).thenReturn(false);
        when(jwtService.isUserAccessToken(claims)).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertFalse(chain.invoked);
        assertEquals(401, response.getStatus());
        assertEquals("Invalid token", response.getErrorMessage());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternalShouldReturnUnauthorizedWhenJwtServiceThrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer broken-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        RecordingFilterChain chain = new RecordingFilterChain();

        SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("principal", null)
        );

        when(jwtService.parse("broken-token"))
            .thenThrow(new InvalidJwtException("JWT token is invalid"));

        filter.doFilter(request, response, chain);

        assertFalse(chain.invoked);
        assertEquals(401, response.getStatus());
        assertEquals("Invalid token", response.getErrorMessage());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private static class RecordingFilterChain implements FilterChain {

        private boolean invoked;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            this.invoked = true;
        }
    }
}
