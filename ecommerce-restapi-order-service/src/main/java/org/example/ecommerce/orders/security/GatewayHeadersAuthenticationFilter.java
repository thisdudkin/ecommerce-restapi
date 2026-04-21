package org.example.ecommerce.orders.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.ecommerce.orders.exception.security.RestAuthenticationEntryPoint;
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

import static org.example.ecommerce.orders.exception.utils.ExceptionConstants.DETAIL_INVALID_TOKEN;

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

        boolean hasAnyGatewayHeader = StringUtils.hasText(userIdHeader)
            || StringUtils.hasText(roleHeader)
            || StringUtils.hasText(tokenTypeHeader);

        if (!hasAnyGatewayHeader) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(userIdHeader)
            || !StringUtils.hasText(roleHeader)
            || !StringUtils.hasText(tokenTypeHeader)
            || !GatewayHeaderNames.ACCESS_TOKEN_TYPE.equalsIgnoreCase(tokenTypeHeader)) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(DETAIL_INVALID_TOKEN)
            );
            return;
        }

        try {
            Long userId = Long.valueOf(userIdHeader);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                new UserSecurityPrincipal(userId, "gateway", false),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + roleHeader))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (NumberFormatException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(DETAIL_INVALID_TOKEN, e)
            );
        }
    }

}
