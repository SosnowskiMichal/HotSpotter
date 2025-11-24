package pwr.zpi.hotspotter.sonar.model.fileanalysis;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SonarIssueLocation {
    private TextRange textRange;
    private String filePath;
    private String message;
    private Map<String, String> messageTranslations = new HashMap<>();
}
