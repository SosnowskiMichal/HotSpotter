package pwr.zpi.hotspotter.sonar.model.fileanalysis;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "sonar_file_analysis_results")
public class SonarFileAnalysisResult {
    @Id
    private String repoAnalysisId;
    @NonNull
    private String projectKey;
    private String projectName;
    private LocalDateTime analysisDate;
    private List<SonarIssue> issues;
}

