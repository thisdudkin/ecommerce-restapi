package org.example.ecommerce.auth.security.service;

import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.repository.UserCredentialRepository;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.principal.AuthUserDetails;
import org.example.ecommerce.auth.utils.TestDataGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserDetailsServiceTests {

    @Mock
    private UserCredentialRepository credentialRepository;

    @InjectMocks
    private JpaUserDetailsService userDetailsService;

    @Test
    void loadUserByUsernameShouldReturnAuthUserDetails() {
        UserCredential credential = TestDataGenerator.userCredential(
            101L,
            "alex.user",
            true,
            Role.USER
        );

        when(credentialRepository.findByLogin("alex.user"))
            .thenReturn(Optional.of(credential));

        UserDetails actual = userDetailsService.loadUserByUsername("alex.user");

        AuthUserDetails userDetails = assertInstanceOf(AuthUserDetails.class, actual);
        assertEquals("alex.user", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertEquals(Role.USER, userDetails.getRole());

        verify(credentialRepository).findByLogin("alex.user");
    }

    @Test
    void loadUserByUsernameShouldThrowWhenCredentialDoesNotExist() {
        when(credentialRepository.findByLogin("missing.user"))
            .thenReturn(Optional.empty());

        assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("missing.user")
        );

        verify(credentialRepository).findByLogin("missing.user");
    }

}
