package pwr.zpi.hotspotter.authentication.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.authentication.config.GoogleProperties;
import pwr.zpi.hotspotter.user.model.User;
import pwr.zpi.hotspotter.user.repository.UserRepository;
import pwr.zpi.hotspotter.authentication.service.JwtService;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final GoogleProperties googleProperties;
    private final CookieUtil cookieUtil;


    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    newUser.setName(name);
                    newUser.setProvider(User.AuthProvider.GOOGLE);
                    newUser.setProviderId(providerId);
                    newUser.setRole(User.Role.USER);
                    return userRepository.save(newUser);
                });

        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        String token = jwtService.generateToken(user);
        cookieUtil.addCookie(response, cookieUtil.createJwtCookie(token, request));

        String redirectUrl = googleProperties.getRedirectUri();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}