package pwr.zpi.hotspotter.repositorymanagement.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "repositories")
public class RepositoryManagementConfig {

    @NotBlank(message = "Base directory for repositories must be configured")
    private String baseDirectory;

    @NotNull
    @Min(value = 1, message = "Minimum free space must be at least 1 GB")
    private Long minFreeSpaceGb = 10L;

    @NotNull
    private CleanupStrategy cleanupStrategy = CleanupStrategy.LEAST_RECENTLY_USED;

    public enum CleanupStrategy {
        LEAST_RECENTLY_USED,
        LEAST_FREQUENTLY_USED
    }

    public long getMinFreeSpaceInBytes() {
        return minFreeSpaceGb * 1024L * 1024L * 1024L;
    }

}
