package pwr.zpi.hotspotter.authentication.component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.authentication.config.JwtProperties;

import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieUtil {
    private final JwtProperties jwtProperties;

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String JWT_COOKIE_PATH = "/";
    private static final String SAME_SITE_POLICY = "Lax";
    private static final boolean SECURE_FLAG = false;

    public Cookie createJwtCookie(String token) {
        return createCookie(
                JWT_COOKIE_NAME,
                token,
                (int) (jwtProperties.getExpiration() / 1000),
                JWT_COOKIE_PATH
        );
    }

    public Cookie deleteJwtCookie() {
        return createCookie(JWT_COOKIE_NAME, null, 0, JWT_COOKIE_PATH);
    }

    public Optional<String> getJwtFromCookies(HttpServletRequest request) {
        return getCookieValue(request, JWT_COOKIE_NAME);
    }

    @SuppressWarnings("SameParameterValue")
    private Cookie createCookie(String name, String value, int maxAge, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(SECURE_FLAG);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);

        cookie.setAttribute("SameSite", SAME_SITE_POLICY);

        return cookie;
    }

    @SuppressWarnings("SameParameterValue")
    private Optional<String> getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void addCookie(HttpServletResponse response, Cookie cookie) {
        response.addCookie(cookie);
    }

    public void addCookies(HttpServletResponse response, Cookie... cookies) {
        for (Cookie cookie : cookies) {
            response.addCookie(cookie);
        }
    }
}
