package pwr.zpi.hotspotter.sonar.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SonarIssueDTO(
        String path,
        Integer startLine,
        Integer endLine,
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
            Integer startLine,
            Integer endLine
    ) { }
}
