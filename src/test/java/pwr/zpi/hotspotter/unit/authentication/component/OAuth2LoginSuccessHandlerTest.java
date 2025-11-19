package pwr.zpi.hotspotter.unit.authentication.component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import pwr.zpi.hotspotter.authentication.component.CookieUtil;
import pwr.zpi.hotspotter.authentication.component.OAuth2LoginSuccessHandler;
import pwr.zpi.hotspotter.authentication.config.GoogleProperties;
import pwr.zpi.hotspotter.authentication.service.JwtService;
import pwr.zpi.hotspotter.user.model.User;
import pwr.zpi.hotspotter.user.repository.UserRepository;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private GoogleProperties googleProperties;
    @Mock
    private CookieUtil cookieUtil;

    @InjectMocks
    private OAuth2LoginSuccessHandler handler;

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final Authentication authentication = mock(Authentication.class);

    @Test
    void createsNewUserWhenEmailNotExists() throws IOException {
        when(googleProperties.getRedirectUri()).thenReturn("/home");

        var redirectStrategy = mock(RedirectStrategy.class);
        handler.setRedirectStrategy(redirectStrategy);

        OAuth2User oAuth = mock(OAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(oAuth);

        when(oAuth.getAttribute("email")).thenReturn("new@example.com");
        when(oAuth.getAttribute("name")).thenReturn("New User");
        when(oAuth.getAttribute("sub")).thenReturn("123");

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        User saved = new User();
        saved.setEmail("new@example.com");
        saved.setName("New User");

        when(userRepository.save(any(User.class))).thenReturn(saved);

        when(jwtService.generateToken(saved)).thenReturn("JWT123");

        Cookie cookie = new Cookie("jwt", "JWT123");
        when(cookieUtil.createJwtCookie("JWT123", request)).thenReturn(cookie);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("new@example.com") &&
                        u.getName().equals("New User") &&
                        u.getProviderId().equals("123") &&
                        u.getProvider() == User.AuthProvider.GOOGLE
        ));

        verify(cookieUtil).addCookie(response, cookie);

        verify(redirectStrategy).sendRedirect(request, response, "/home");
    }

    @Test
    void updatesNameWhenChanged() throws IOException {
        when(googleProperties.getRedirectUri()).thenReturn("/dashboard");

        var redirectStrategy = mock(org.springframework.security.web.RedirectStrategy.class);
        handler.setRedirectStrategy(redirectStrategy);

        OAuth2User oAuth = mock(OAuth2User.class);
        when(authentication.getPrincipal()).thenReturn(oAuth);

        when(oAuth.getAttribute("email")).thenReturn("existing@example.com");
        when(oAuth.getAttribute("name")).thenReturn("Updated Name");

        User existing = new User();
        existing.setEmail("existing@example.com");
        existing.setName("Old Name");

        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));

        when(jwtService.generateToken(existing)).thenReturn("JWT999");

        Cookie cookie = new Cookie("jwt", "JWT999");
        when(cookieUtil.createJwtCookie("JWT999", request)).thenReturn(cookie);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository).save(existing);
        assertThat(existing.getName()).isEqualTo("Updated Name");
        verify(cookieUtil).addCookie(eq(response), eq(cookie));

        verify(redirectStrategy).sendRedirect(request, response, "/dashboard");
    }
}
