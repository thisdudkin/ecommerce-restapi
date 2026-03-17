package org.example.ecommerce.users.security.jwt;

import org.example.ecommerce.users.repository.UserRepository;
import org.example.ecommerce.users.security.principal.UserSecurityPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessGuardTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccessGuard accessGuard;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void canAccessUserShouldReturnFalseWhenAuthenticationIsNull() {
        boolean actual = accessGuard.canAccessUser(101L);

        assertFalse(actual);
        verifyNoInteractions(userRepository);
    }

    @Test
    void canAccessUserShouldReturnFalseWhenPrincipalHasWrongType() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("principal", null, List.of())
        );

        boolean actual = accessGuard.canAccessUser(101L);

        assertFalse(actual);
        verifyNoInteractions(userRepository);
    }

    @Test
    void canAccessUserShouldReturnFalseWhenPrincipalIsInternal() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UserSecurityPrincipal(null, "authentication-service", true),
                null,
                List.of()
            )
        );

        boolean actual = accessGuard.canAccessUser(101L);

        assertFalse(actual);
        verifyNoInteractions(userRepository);
    }

    @Test
    void canAccessUserShouldReturnFalseWhenPrincipalUserIdIsNull() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UserSecurityPrincipal(null, "alex.user", false),
                null,
                List.of()
            )
        );

        boolean actual = accessGuard.canAccessUser(101L);

        assertFalse(actual);
        verifyNoInteractions(userRepository);
    }

    @Test
    void canAccessUserShouldReturnFalseWhenUserIdDoesNotMatch() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UserSecurityPrincipal(202L, "alex.user", false),
                null,
                List.of()
            )
        );

        boolean actual = accessGuard.canAccessUser(101L);

        assertFalse(actual);
        verifyNoInteractions(userRepository);
    }

    @Test
    void canAccessUserShouldReturnFalseWhenUserIsInactive() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UserSecurityPrincipal(101L, "alex.user", false),
                null,
                List.of()
            )
        );
        when(userRepository.existsByIdAndActiveTrue(101L)).thenReturn(false);

        boolean actual = accessGuard.canAccessUser(101L);

        assertFalse(actual);
        verify(userRepository).existsByIdAndActiveTrue(101L);
    }

    @Test
    void canAccessUserShouldReturnTrueWhenUserOwnsResourceAndIsActive() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new UserSecurityPrincipal(101L, "alex.user", false),
                null,
                List.of()
            )
        );
        when(userRepository.existsByIdAndActiveTrue(101L)).thenReturn(true);

        boolean actual = accessGuard.canAccessUser(101L);

        assertTrue(actual);
        verify(userRepository).existsByIdAndActiveTrue(101L);
    }
}
