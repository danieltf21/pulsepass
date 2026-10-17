package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void continuesChainWhenNoBearerHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatesWithValidBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.parseUserId("valid.token.here")).thenReturn(1L);
        UserDetails details = new SecurityPrincipal(1L, "andrea", "andrea@email.com", "hash", true, Set.of("USER"));
        when(userDetailsService.loadByUserId(1L)).thenReturn(details);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(details);
    }

    @Test
    void rejectsInvalidBearerWithout500() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer garbage");
        when(jwtService.parseUserId("garbage")).thenThrow(new io.jsonwebtoken.MalformedJwtException("bad token"));
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void doesNotOverwriteExistingAuthentication() throws Exception {
        UsernamePasswordAuthenticationToken existing =
                new UsernamePasswordAuthenticationToken("someone", null);
        SecurityContextHolder.getContext().setAuthentication(existing);

        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token.here");
        when(jwtService.parseUserId("valid.token.here")).thenReturn(1L);
        UserDetails details = new SecurityPrincipal(1L, "andrea", "andrea@email.com", "hash", true, Set.of("USER"));
        when(userDetailsService.loadByUserId(1L)).thenReturn(details);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    }
}