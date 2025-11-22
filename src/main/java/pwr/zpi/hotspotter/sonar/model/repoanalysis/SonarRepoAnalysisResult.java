package pwr.zpi.hotspotter.sonar.model.repoanalysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sonar_repo_analysis_results")
public class SonarRepoAnalysisResult {
    @Id
    private String repoAnalysisId;
    private LocalDateTime analysisDate;
    private Integer bugs;
    private Integer vulnerabilities;
    private Integer codeSmells;
    private Double coverage;
    private Double duplicatedLinesDensity;
    private Integer complexity;
}
