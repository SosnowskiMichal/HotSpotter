package pwr.zpi.hotspotter.sonar.model.repoanalysis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sonar_repo_analysis_components")
@CompoundIndex(name = "repoAnalysisId_path_idx", def = "{'repoAnalysisId': 1, 'path': 1}", unique = true)
public class SonarRepoAnalysisComponent {
    @Id
    private String id;
    private String repoAnalysisId;
    private String qualifier;
    private String path;

    private Integer bugs;
    private Integer vulnerabilities;
    private Integer codeSmells;
    private Integer complexity;
    private Double coverage;
    private Double duplicatedLinesDensity;
}
