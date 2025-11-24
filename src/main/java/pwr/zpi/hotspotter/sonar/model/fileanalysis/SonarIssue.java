package pwr.zpi.hotspotter.sonar.model.fileanalysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sonar_issues")
@CompoundIndex(name = "repoAnalysisId_path_idx", def = "{'repoAnalysisId': 1, 'path': 1}")
public class SonarIssue {
    @Id
    private String id;
    @NonNull
    private String repoAnalysisId;
    @NonNull
    private String path;
    private TextRange textRange;
    private String severity;
    private String message;
    private Map<String, String> messageTranslations = new HashMap<>();
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
