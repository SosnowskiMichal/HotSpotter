package pwr.zpi.hotspotter.authentication.component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.authentication.config.JwtProperties;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CookieUtil {
    private final JwtProperties jwtProperties;

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String JWT_COOKIE_PATH = "/";
    private static final String SAME_SITE_POLICY = "Lax";

    public Cookie createJwtCookie(String token, HttpServletRequest request) {
        return createCookie(
                JWT_COOKIE_NAME,
                token,
                (int) (jwtProperties.getExpiration() / 1000),
                JWT_COOKIE_PATH,
                request
        );
    }

    public Cookie deleteJwtCookie(HttpServletRequest request) {
        return createCookie(JWT_COOKIE_NAME, null, 0, JWT_COOKIE_PATH, request);
    }

    public Optional<String> getJwtFromCookies(HttpServletRequest request) {
        return getCookieValue(request, JWT_COOKIE_NAME);
    }

    @SuppressWarnings("SameParameterValue")
    private Cookie createCookie(String name, String value, int maxAge, String path, HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String domain = determineDomain(origin);
        boolean shouldBeSecure = determineSecure(origin);

        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(shouldBeSecure);
        cookie.setPath(path);
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", SAME_SITE_POLICY);

        if (domain != null && !domain.isEmpty()) {
            cookie.setDomain(domain);
        }

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

    private String determineDomain(String origin) {
        if (origin == null) {
            return null;
        }

        try {
            URI uri = new URI(origin);
            String host = uri.getHost();

            if (isLocalhost(host)) {
                return null;
            }

            return extractBaseDomain(host);

        } catch (Exception e) {
            return null;
        }
    }

    private boolean isLocalhost(String host) {
        if (host == null) return false;

        return host.equals("localhost")
                || host.equals("127.0.0.1")
                || host.equals("0.0.0.0")
                || host.equals("::1")
                || host.startsWith("192.168.")
                || host.startsWith("10.")
                || host.startsWith("172.16.");
    }

    private String extractBaseDomain(String host) {
        if (host == null) return null;

        if (host.endsWith(jwtProperties.getDomain())) {
            return jwtProperties.getDomain();
        }

        return null;
    }

    private boolean determineSecure(String origin) {
        if (origin == null) return jwtProperties.isSecure();

        if (origin.startsWith("http://localhost") || origin.startsWith("http://127.0.0.1")) {
            return false;
        }

        if (origin.startsWith("https://")) {
            return true;
        }

        return jwtProperties.isSecure();
    }
}
