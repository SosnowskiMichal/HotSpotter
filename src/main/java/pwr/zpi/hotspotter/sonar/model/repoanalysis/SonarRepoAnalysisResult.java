package pwr.zpi.hotspotter.sonar.model.repoanalysis;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "sonar_repo_analysis_results")
public class SonarRepoAnalysisResult {
    @Id
    private String id;
    @NonNull
    private String projectKey;
    private String projectName;
    private LocalDateTime analysisDate;
    private Integer bugs;
    private Integer vulnerabilities;
    private Integer codeSmells;
    private Double coverage;
    private Double duplicatedLinesDensity;
    private Integer complexity;
    private List<SonarRepoAnalysisComponent> components;
}
