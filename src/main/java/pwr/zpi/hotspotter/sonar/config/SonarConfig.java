package pwr.zpi.hotspotter.sonar.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Configuration
public class SonarConfig {

    @Value("${sonar.host.url}")
    private String sonarUrl;

    @Value("${sonar.login}")
    private String login;

    @Value("${sonar.password}")
    private String password;

    private final RestTemplate restTemplate;

    public SonarConfig() {
        this.restTemplate = new RestTemplate();
    }

    public String generateToken(String tokenName) {
        try {
            String auth = login + ":" + password;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);

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
}
