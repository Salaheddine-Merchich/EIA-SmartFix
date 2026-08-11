package com.ocp.eia.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private AuthCookieService authCookieService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bearerToken_authenticates() throws Exception {
        UserDetails user = User.withUsername("admin@ocp.ma").password("x").roles("ADMIN").build();
        when(request.getHeader("Authorization")).thenReturn("Bearer good.token");
        when(jwtService.extractUsername("good.token")).thenReturn("admin@ocp.ma");
        when(userDetailsService.loadUserByUsername("admin@ocp.ma")).thenReturn(user);
        when(jwtService.isAccessToken("good.token")).thenReturn(true);
        when(jwtService.isTokenValid("good.token", user)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
        verify(request, never()).getParameter("access_token");
    }

    @Test
    void queryAccessToken_isIgnored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(authCookieService.readCookie(request, AuthCookieService.ACCESS_COOKIE)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(any());
        verify(request, never()).getParameter(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
