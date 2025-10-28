package pwr.zpi.hotspotter.sonar.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pwr.zpi.hotspotter.sonar.config.SonarConfig;

import java.util.Map;

@Slf4j
@Service
public class SonarService {
    @Value("${sonar.host.url}")
    private String sonarUrl;

    private String sonarToken;

    private final RestTemplate restTemplate;
    private final SonarConfig sonarConfig;

    public SonarService(RestTemplateBuilder restTemplateBuilder, SonarConfig sonarConfig) {
        this.sonarConfig = sonarConfig;
        this.restTemplate = restTemplateBuilder.build();
    }

    public boolean validateToken() {
        if (sonarToken == null || sonarToken.isEmpty()) {
            return false;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + sonarToken);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = sonarUrl + "/api/authentication/validate";

            var response = restTemplate.exchange(
                    url,
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

    public boolean setNewSonarToken() {
        String tokenName = "hotspotter-token-" + System.currentTimeMillis();
        this.sonarToken = sonarConfig.generateToken(tokenName);

        return this.sonarToken != null;
    }

    public boolean prepareConnection() {
        if (validateToken()) {
            return true;
        } else {
            return setNewSonarToken();
        }
    }
}
