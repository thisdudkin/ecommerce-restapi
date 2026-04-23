package org.example.ecommerce.payments.infrastructure.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long userId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("No authenticated user found");
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            throw new BadCredentialsException("Anonymous authentication is not supported");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserSecurityPrincipal userPrincipal)) {
            throw new BadCredentialsException("Unexpected principal type");
        }

        if (userPrincipal.userId() == null) {
            throw new BadCredentialsException("Authenticated user id is missing");
        }

        return userPrincipal.userId();
    }

}
