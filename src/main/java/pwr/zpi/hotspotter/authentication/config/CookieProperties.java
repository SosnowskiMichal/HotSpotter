package pwr.zpi.hotspotter.authentication.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {
    private boolean secure;
    private String sameSite;
    private String domain;
}
