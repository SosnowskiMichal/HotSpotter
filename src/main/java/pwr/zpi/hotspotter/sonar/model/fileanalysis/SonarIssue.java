package pwr.zpi.hotspotter.sonar.model.fileanalysis;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SonarIssue {
    private String filePath;
    private TextRange textRange;
    private String severity;
    private String message;
    private String type;
    private String rule;
    private String effort;
    private String debt;
    private String authorEmail;
    private List<String> tags;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private List<SonarIssueLocation> locations;
}
