package pwr.zpi.hotspotter.unit.authentication.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.authentication.config.JwtProperties;
import pwr.zpi.hotspotter.authentication.service.JwtService;
import pwr.zpi.hotspotter.user.model.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(jwtProperties.getExpiration()).thenReturn(3600000L);
        when(jwtProperties.getSecret()).thenReturn("YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXo1Njc4OTAxMjM=");
    }

    private User sampleUser() {
        User u = new User();
        u.setId(String.valueOf(10));
        u.setEmail("test@example.com");
        u.setRole(User.Role.USER);
        u.setPassword("pass");
        return u;
    }

    @Test
    void generateToken_shouldContainAllClaims() {
        User user = sampleUser();

        String token = jwtService.generateToken(user);

        String email = jwtService.extractEmail(token);
        String userId = jwtService.extractUserId(token);

        assertThat(email).isEqualTo("test@example.com");
        assertThat(userId).isEqualTo("10");
    }

    @Test
    void extractClaim_shouldReturnCorrectValues() {
        User user = sampleUser();
        String token = jwtService.generateToken(user);

        Claims claims = jwtService.extractClaim(token, c -> c);

        assertThat(claims.get("email")).isEqualTo("test@example.com");
        assertThat(claims.get("role")).isEqualTo("USER");
        assertThat(claims.getSubject()).isEqualTo("test@example.com");
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {
        User user = sampleUser();
        String token = jwtService.generateToken(user);

        org.springframework.security.core.userdetails.User springUser =
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        java.util.Collections.emptyList()
                );

        assertThat(jwtService.isTokenValid(token, springUser)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalseForDifferentUser() {
        User user = sampleUser();
        String token = jwtService.generateToken(user);

        org.springframework.security.core.userdetails.User other =
                new org.springframework.security.core.userdetails.User(
                        "other@example.com",
                        user.getPassword(),
                        java.util.Collections.emptyList()
                );

        assertThat(jwtService.isTokenValid(token, other)).isFalse();
    }

    @Test
    void extractAllClaims_shouldWorkWithValidToken() {
        User user = sampleUser();
        String token = jwtService.generateToken(user);

        Claims claims = jwtService.extractClaim(token, c -> c);

        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo("test@example.com");
    }
}
