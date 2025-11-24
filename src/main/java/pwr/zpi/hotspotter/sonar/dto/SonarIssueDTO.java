package pwr.zpi.hotspotter.sonar.dto;

import pwr.zpi.hotspotter.sonar.model.fileanalysis.TextRange;

import java.time.LocalDateTime;
import java.util.List;

public record SonarIssueDTO(
        String path,
        TextRange textRange,
        String severity,
        String message,
        String type,
        String rule,
        String effort,
        String debt,
        String authorEmail,
        List<String> tags,
        LocalDateTime creationDate,
        LocalDateTime updateDate,
        List<SonarIssueLocationDTO> locations
) {
    public record SonarIssueLocationDTO(
            String message,
            TextRange textRange
    ) { }
}
