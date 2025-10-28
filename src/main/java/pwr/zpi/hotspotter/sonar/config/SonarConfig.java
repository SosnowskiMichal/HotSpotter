package pwr.zpi.hotspotter.sonar.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Configuration
public class SonarConfig {

    @Value("${sonar.host.url}")
    private String sonarUrl;

    private static final String DEFAULT_LOGIN = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    private final RestTemplate restTemplate;

    public SonarConfig(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public String generateToken(String tokenName) {
        try {
            String auth = DEFAULT_LOGIN + ":" + DEFAULT_PASSWORD;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            URI uri = UriComponentsBuilder
                    .fromUriString(sonarUrl)
                    .path("/api/user_tokens/generate")
                    .queryParam("name", tokenName)
                    .build()
                    .encode()
                    .toUri();

            var response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object token = response.getBody().get("token");
                return token != null ? token.toString() : null;
            } else {
                log.warn("Failed to generate Sonar token, status: {}", response.getStatusCode());
                return null;
            }
        } catch (RestClientException e) {
            log.error("Error while generating Sonar token", e);
            return null;
        }
    }

    public boolean validateToken(String sonarToken) {
        if (sonarToken == null || sonarToken.isEmpty()) {
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + sonarToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            URI uri = UriComponentsBuilder
                    .fromUriString(sonarUrl)
                    .path("/api/authentication/validate")
                    .build()
                    .encode()
                    .toUri();

            var response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getStatusCode().is2xxSuccessful()
                    && Boolean.TRUE.equals(response.getBody() != null ? response.getBody().get("valid") : null);

        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("Unauthorized: Sonar token is invalid or expired.");
            return false;
        } catch (Exception e) {
            log.error("Error validating Sonar token: {}", e.getMessage());
            return false;
        }
    }

    public void logIn() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("login", DEFAULT_LOGIN);
            body.add("password", DEFAULT_PASSWORD);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            URI uri = UriComponentsBuilder
                    .fromUriString(sonarUrl)
                    .path("/api/authentication/login")
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<String> response = restTemplate.postForEntity(uri, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Sonar login successful.");
            } else {
                log.error("Sonar login failed, status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error during Sonar login: {}", e.getMessage());
        }
    }
}
