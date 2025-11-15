package pwr.zpi.hotspotter.sonar.model.fileanalysis;

import lombok.Data;

@Data
public class SonarIssueLocation {
    private TextRange textRange;
    private String filePath;
    private String message;
}
