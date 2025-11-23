package pwr.zpi.hotspotter.google.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {
    private String redirectUri;
    private String projectId;
}
