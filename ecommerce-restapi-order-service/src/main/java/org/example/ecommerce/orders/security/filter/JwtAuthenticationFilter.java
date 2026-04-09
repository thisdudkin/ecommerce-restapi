package org.example.ecommerce.orders.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ecommerce.orders.exception.custom.jwt.InvalidJwtException;
import org.example.ecommerce.orders.exception.security.RestAuthenticationEntryPoint;
import org.example.ecommerce.orders.security.jwt.JwtService;
import org.example.ecommerce.orders.security.jwt.VerifiedJwtClaims;
import org.example.ecommerce.orders.security.principal.UserSecurityPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.DETAIL_INVALID_TOKEN;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService, RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
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

            if (!jwtService.isUserAccessToken(claims)) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException(DETAIL_INVALID_TOKEN)
                );
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new UserSecurityPrincipal(claims.userId(), claims.subject(), false),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidJwtException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(DETAIL_INVALID_TOKEN, e)
            );
        }
    }

}
