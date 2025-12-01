package pwr.zpi.hotspotter.fileanalysis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "file-analysis")
public class FileAnalysisConfig {

    private String baseDirectory;

}
