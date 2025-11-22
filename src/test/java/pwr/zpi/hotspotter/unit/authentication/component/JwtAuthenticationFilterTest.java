package pwr.zpi.hotspotter.unit.authentication.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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
import pwr.zpi.hotspotter.authentication.component.JwtAuthenticationFilter;
import pwr.zpi.hotspotter.authentication.service.CustomUserDetailsService;
import pwr.zpi.hotspotter.authentication.service.JwtService;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain filterChain = mock(FilterChain.class);

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUserFromCookieJwt() throws IOException, ServletException {
        Cookie jwtCookie = new Cookie("jwt", "TOKEN123");
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

        when(jwtService.extractEmail("TOKEN123")).thenReturn("user@example.com");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getAuthorities()).thenReturn(List.of());
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);

        when(jwtService.isTokenValid("TOKEN123", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authenticatesUserFromAuthorizationHeader() throws IOException, ServletException {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer TOKEN456");

        when(jwtService.extractEmail("TOKEN456")).thenReturn("user2@example.com");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getAuthorities()).thenReturn(List.of());
        when(userDetailsService.loadUserByUsername("user2@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid("TOKEN456", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenJwtMissing() throws IOException, ServletException {
        when(request.getCookies()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doesNotAuthenticateWhenTokenInvalid() throws IOException, ServletException {
        Cookie jwtCookie = new Cookie("jwt", "BROKEN");
        when(request.getCookies()).thenReturn(new Cookie[]{jwtCookie});

        when(jwtService.extractEmail("BROKEN")).thenThrow(new RuntimeException("Invalid"));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
