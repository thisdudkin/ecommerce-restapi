package org.example.ecommerce.auth.service.auth;

import org.example.ecommerce.auth.client.UserClient;
import org.example.ecommerce.auth.dto.request.LoginRequest;
import org.example.ecommerce.auth.dto.request.RefreshTokenRequest;
import org.example.ecommerce.auth.dto.request.RegisterRequest;
import org.example.ecommerce.auth.dto.request.ValidateTokenRequest;
import org.example.ecommerce.auth.dto.response.TokenResponse;
import org.example.ecommerce.auth.dto.response.UserResponse;
import org.example.ecommerce.auth.dto.response.ValidateTokenResponse;
import org.example.ecommerce.auth.entity.UserCredential;
import org.example.ecommerce.auth.exception.custom.CredentialAlreadyExistsException;
import org.example.ecommerce.auth.exception.custom.CredentialNotFoundException;
import org.example.ecommerce.auth.exception.custom.InactiveUserCredentialException;
import org.example.ecommerce.auth.repository.UserCredentialRepository;
import org.example.ecommerce.auth.security.enums.JwtType;
import org.example.ecommerce.auth.security.enums.Role;
import org.example.ecommerce.auth.security.exception.InvalidJwtException;
import org.example.ecommerce.auth.security.exception.InvalidRefreshTokenException;
import org.example.ecommerce.auth.security.jwt.JwtClaims;
import org.example.ecommerce.auth.security.jwt.JwtService;
import org.example.ecommerce.auth.security.principal.AuthUserDetails;
import org.example.ecommerce.auth.service.registration.RegistrationCompensationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class AuthService {

    private final UserClient userClient;
    private final JwtService jwtService;
    private final TokenIssuer tokenIssuer;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserCredentialRepository credentialRepository;
    private final RegistrationCompensationService registrationCompensationService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserClient userClient,
                       JwtService jwtService,
                       TokenIssuer tokenIssuer,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       UserCredentialRepository credentialRepository,
                       RegistrationCompensationService registrationCompensationService,
                       RefreshTokenService refreshTokenService) {
        this.userClient = userClient;
        this.jwtService = jwtService;
        this.tokenIssuer = tokenIssuer;
        this.passwordEncoder = passwordEncoder;
        this.credentialRepository = credentialRepository;
        this.authenticationManager = authenticationManager;
        this.registrationCompensationService = registrationCompensationService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (credentialRepository.existsByLogin(request.login()))
            throw new CredentialAlreadyExistsException("Credential with login '%s' already exists".formatted(request.login()));

        UserResponse createdUser = userClient.create(request.user());

        try {
            UserCredential savedCredential = credentialRepository.saveAndFlush(
                UserCredential.builder()
                    .userId(createdUser.id())
                    .login(request.login())
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .role(Role.USER)
                    .active(true)
                    .build()
            );

            return tokenIssuer.issue(savedCredential);
        } catch (DataIntegrityViolationException e) {
            registrationCompensationService.rollbackUserCreation(createdUser.id(), e);

            throw new CredentialAlreadyExistsException("Credential with login '%s' or user id '%s' already exists"
                .formatted(request.login(), createdUser.id()));

        } catch (RuntimeException e) {
            registrationCompensationService.rollbackUserCreation(createdUser.id(), e);
            throw e;
        }
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.login(), request.password())
        );

        AuthUserDetails principal = Objects.requireNonNull((AuthUserDetails) authentication.getPrincipal());
        return tokenIssuer.issue(principal);
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        JwtClaims claims = jwtService.parseRefreshToken(request.refreshToken());
        refreshTokenService.validate(claims.userId(), request.refreshToken());

        UserCredential credential = credentialRepository.findByUserId(claims.userId())
            .orElseThrow(() -> new CredentialNotFoundException(
                "Credential for user id '%s' not found".formatted(claims.userId())
            ));

        if (!credential.getActive())
            throw new InactiveUserCredentialException("Credential with user id '%s' is inactive".formatted(claims.userId()));

        refreshTokenService.revoke(request.refreshToken());
        return tokenIssuer.issue(credential);
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        JwtClaims claims = jwtService.parseRefreshToken(request.refreshToken());
        refreshTokenService.validate(claims.userId(), request.refreshToken());
        refreshTokenService.revoke(request.refreshToken());
    }

    @Transactional(readOnly = true)
    public ValidateTokenResponse validate(ValidateTokenRequest request) {
        try {
            JwtClaims claims = jwtService.parse(request.token());

            if (claims.tokenType() == JwtType.REFRESH && !refreshTokenService.isActive(request.token()))
                return ValidateTokenResponse.invalid();

            return credentialRepository.findByUserId(claims.userId())
                .filter(UserCredential::getActive)
                .map(credential -> new ValidateTokenResponse(
                    true,
                    claims.userId(),
                    claims.role(),
                    claims.tokenType()
                ))
                .orElse(ValidateTokenResponse.invalid());
        } catch (InvalidJwtException | InvalidRefreshTokenException e) {
            return ValidateTokenResponse.invalid();
        }
    }

}
