package pwr.zpi.hotspotter.repositoryanalysis.logprocessing.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "log-extraction")
public class LogExtractorConfig {

    @NotBlank(message = "Log directory name is required")
    private String logDirectoryName = "logs";

    @Min(value = 1, message = "Monitoring interval must be at least 1 second")
    private Integer processMonitoringIntervalSeconds = 20;

    @Min(value = 1, message = "Process timeout must be at least 1 minute")
    private Integer processTimeoutMinutes = 15;

}
