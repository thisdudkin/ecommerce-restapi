package org.example.ecommerce.users.security.jwt;

import org.example.ecommerce.users.repository.UserRepository;
import org.example.ecommerce.users.security.principal.UserSecurityPrincipal;
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

        if (authentication == null || !(authentication.getPrincipal() instanceof UserSecurityPrincipal principal)) {
            return false;
        }

        if (principal.internal() || principal.userId() == null) {
            return false;
        }

        return userId.equals(principal.userId())
            && userRepository.existsByIdAndActiveTrue(userId);
    }

}
