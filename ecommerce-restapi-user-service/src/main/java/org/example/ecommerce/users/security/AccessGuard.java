package org.example.ecommerce.users.security;

import org.example.ecommerce.users.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

    private final UserRepository userRepository;

    public AccessGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean canAccessUser(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserSecurityPrincipal userPrincipal)) {
            return false;
        }

        Long authenticatedUserId = userPrincipal.userId();
        if (authenticatedUserId == null) {
            return false;
        }

        if (!authenticatedUserId.equals(userId)) {
            return false;
        }

        return userRepository.existsByIdAndActiveTrue(userId);
    }

}
