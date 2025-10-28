package pwr.zpi.hotspotter.sonar.service;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pwr.zpi.hotspotter.sonar.config.SonarConfig;

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

    @Synchronized
    public boolean prepareConnection() {
        if (sonarConfig.validateToken(this.sonarToken)) {
            return true;
        } else {
            sonarConfig.logIn();
            return setNewSonarToken();
        }
    }

    private boolean setNewSonarToken() {
        String tokenName = "hotspotter-token-" + System.currentTimeMillis();
        this.sonarToken = sonarConfig.generateToken(tokenName);

        return this.sonarToken != null;
    }
}
