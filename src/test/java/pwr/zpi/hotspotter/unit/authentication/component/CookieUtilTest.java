package pwr.zpi.hotspotter.unit.authentication.component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.authentication.component.CookieUtil;
import pwr.zpi.hotspotter.authentication.config.CookieProperties;
import pwr.zpi.hotspotter.authentication.config.JwtProperties;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookieUtilTest {

    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private CookieProperties cookieProperties;

    @InjectMocks
    private CookieUtil cookieUtil;

    @Test
    void createsJwtCookieWithCorrectAttributes() {
        when(jwtProperties.getExpiration()).thenReturn(60000L);
        when(cookieProperties.getSameSite()).thenReturn("Strict");
        when(cookieProperties.getDomain()).thenReturn("example.com");

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://app.example.com");

        Cookie cookie = cookieUtil.createJwtCookie("TOKEN123", req);

        assertThat(cookie.getName()).isEqualTo("jwt");
        assertThat(cookie.getValue()).isEqualTo("TOKEN123");
        assertThat(cookie.getMaxAge()).isEqualTo(60);
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(cookie.getDomain()).isEqualTo("example.com");
    }

    @Test
    void deleteJwtCookieSetsExpirationZero() {
        when(cookieProperties.getSameSite()).thenReturn("Strict");
        when(cookieProperties.getDomain()).thenReturn("example.com");

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("https://app.example.com");

        Cookie cookie = cookieUtil.deleteJwtCookie(req);

        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getValue()).isNull();
    }

    @Test
    void getJwtFromCookiesReturnsValueIfPresent() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        Cookie jwt = new Cookie("jwt", "XYZ");
        when(req.getCookies()).thenReturn(new Cookie[]{jwt});

        Optional<String> result = cookieUtil.getJwtFromCookies(req);

        assertThat(result).contains("XYZ");
    }

    @Test
    void getJwtFromCookiesReturnsEmptyIfMissing() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getCookies()).thenReturn(null);

        assertThat(cookieUtil.getJwtFromCookies(req)).isEmpty();
    }

    @Test
    void domainIsNullForLocalhost() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Origin")).thenReturn("http://localhost:3000");

        Cookie cookie = cookieUtil.createJwtCookie("X", req);
        assertThat(cookie.getDomain()).isNull();
        assertThat(cookie.getSecure()).isFalse();
    }

    @Test
    void addCookiesAddsAllCookies() {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        Cookie c1 = new Cookie("a", "1");
        Cookie c2 = new Cookie("b", "2");

        cookieUtil.addCookies(resp, c1, c2);

        verify(resp, times(1)).addCookie(c1);
        verify(resp, times(1)).addCookie(c2);
    }
}
