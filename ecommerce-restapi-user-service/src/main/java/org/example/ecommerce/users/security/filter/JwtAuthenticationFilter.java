package org.example.ecommerce.users.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ecommerce.users.exception.custom.InvalidJwtException;
import org.example.ecommerce.users.security.jwt.JwtService;
import org.example.ecommerce.users.security.jwt.VerifiedJwtClaims;
import org.example.ecommerce.users.security.principal.UserSecurityPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            VerifiedJwtClaims claims = jwtService.parse(token);

            UsernamePasswordAuthenticationToken authentication;
            if (jwtService.isTrustedInternalService(claims)) {
                authentication = new UsernamePasswordAuthenticationToken(
                    new UserSecurityPrincipal(null, claims.subject(), true),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_SERVICE"))
                );
            } else if (jwtService.isUserAccessToken(claims)) {
                authentication = new UsernamePasswordAuthenticationToken(
                    new UserSecurityPrincipal(claims.userId(), claims.subject(), false),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
                );
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                return;
            }

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidJwtException e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        }
    }

}
