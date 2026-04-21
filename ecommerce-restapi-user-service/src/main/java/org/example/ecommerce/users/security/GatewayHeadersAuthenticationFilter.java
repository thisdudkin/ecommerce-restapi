package org.example.ecommerce.users.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ecommerce.users.exception.security.RestAuthenticationEntryPoint;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class GatewayHeadersAuthenticationFilter extends OncePerRequestFilter {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public GatewayHeadersAuthenticationFilter(RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String userIdHeader = request.getHeader(GatewayHeaderNames.AUTHENTICATED_USER_ID);
        String roleHeader = request.getHeader(GatewayHeaderNames.AUTHENTICATED_USER_ROLE);
        String tokenTypeHeader = request.getHeader(GatewayHeaderNames.AUTHENTICATED_TOKEN_TYPE);

        boolean hasAnyGatewayHeader =
            StringUtils.hasText(userIdHeader)
                || StringUtils.hasText(roleHeader)
                || StringUtils.hasText(tokenTypeHeader);

        if (!hasAnyGatewayHeader) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(roleHeader) || !StringUtils.hasText(tokenTypeHeader)) {
            reject(request, response, "Incomplete authentication headers");
            return;
        }

        String normalizedRole = roleHeader.trim().toUpperCase();
        String normalizedTokenType = tokenTypeHeader.trim().toUpperCase();

        try {
            if (GatewayHeaderNames.INTERNAL_TOKEN_TYPE.equals(normalizedTokenType)
                && GatewayHeaderNames.INTERNAL_SERVICE_ROLE.equals(normalizedRole)) {

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    new UserSecurityPrincipal(null, normalizedRole),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }

            if (!GatewayHeaderNames.ACCESS_TOKEN_TYPE.equals(normalizedTokenType)) {
                reject(request, response, "Unsupported authenticated token type");
                return;
            }

            if (!StringUtils.hasText(userIdHeader)) {
                reject(request, response, "Missing authenticated user id");
                return;
            }

            Long userId = Long.valueOf(userIdHeader);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                new UserSecurityPrincipal(userId, normalizedRole),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (NumberFormatException ex) {
            reject(request, response, "Invalid authenticated user id");
        }
    }

    private void reject(HttpServletRequest request,
                        HttpServletResponse response,
                        String message) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(
            request,
            response,
            new BadCredentialsException(message)
        );
    }

}
