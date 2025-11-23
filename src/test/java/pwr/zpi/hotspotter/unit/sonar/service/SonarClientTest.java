package pwr.zpi.hotspotter.unit.sonar.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.service.SonarClient;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SonarClientTest {

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private SonarProperties sonarProperties;

    @InjectMocks
    private SonarClient sonarClient;

    @Test
    void generateTokenReturnsTokenWhenRequestIsSuccessful() {
        when(sonarProperties.getLogin()).thenReturn("admin");
        when(sonarProperties.getPassword()).thenReturn("password");
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        Map<String, Object> responseBody = Map.of("token", "generated-token");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        String token = sonarClient.generateToken("test-token");

        assertEquals("generated-token", token);
    }

    @Test
    void generateTokenReturnsNullWhenResponseIsNotSuccessful() {
        when(sonarProperties.getLogin()).thenReturn("admin");
        when(sonarProperties.getPassword()).thenReturn("password");
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());

        String token = sonarClient.generateToken("test-token");

        assertNull(token);
    }

    @Test
    void validateTokenReturnsTrueForValidToken() {
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        Map<String, Object> responseBody = Map.of("valid", true);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        boolean isValid = sonarClient.validateToken("valid-token");

        assertTrue(isValid);
    }

    @Test
    void validateTokenReturnsFalseForInvalidToken() {
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        Map<String, Object> responseBody = Map.of("valid", false);
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        boolean isValid = sonarClient.validateToken("invalid-token");

        assertFalse(isValid);
    }

    @Test
    void validateTokenReturnsFalseWhenTokenIsNullOrEmpty() {
        boolean isValidForNull = sonarClient.validateToken(null);
        boolean isValidForEmpty = sonarClient.validateToken("");

        assertFalse(isValidForNull);
        assertFalse(isValidForEmpty);
    }

    @Test
    void logInLogsInSuccessfullyWhenCredentialsAreValid() {
        when(sonarProperties.getLogin()).thenReturn("admin");
        when(sonarProperties.getPassword()).thenReturn("password");
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(""));

        sonarClient.logIn();

        verify(restTemplate).postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void logInHandlesErrorWhenLoginFails() {
        when(sonarProperties.getLogin()).thenReturn("admin");
        when(sonarProperties.getPassword()).thenReturn("password");
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        when(restTemplate.postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Login failed"));

        sonarClient.logIn();

        verify(restTemplate).postForEntity(any(URI.class), any(HttpEntity.class), eq(String.class));
    }
}