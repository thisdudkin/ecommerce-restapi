package org.example.ecommerce.auth.security.service;

import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.repository.UserCredentialRepository;
import org.example.ecommerce.auth.security.principal.AuthUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UserCredentialRepository credentialRepository;

    public JpaUserDetailsService(UserCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserCredential credential = credentialRepository.findByLogin(username)
            .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        return new AuthUserDetails(credential);
    }

}
