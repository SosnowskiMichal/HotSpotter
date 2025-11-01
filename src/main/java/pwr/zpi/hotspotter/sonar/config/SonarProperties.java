package pwr.zpi.hotspotter.sonar.config;

import lombok.Data;
import lombok.Synchronized;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sonar")
public class SonarProperties {
    private String hostUrl;
    private String login = "admin";
    private String password = "admin";
    private String token;
    private String scannerPath;

    @Synchronized
    public void setToken(String token) {
        this.token = token;
    }
}
