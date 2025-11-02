package pwr.zpi.hotspotter.sonar.model.repoanalysis;

import lombok.Data;

@Data
public class SonarRepoAnalysisComponent {
    private String key;
    private String name;
    private String qualifier;
    private String path;

    private Integer bugs;
    private Integer vulnerabilities;
    private Integer codeSmells;
    private Integer complexity;
    private Double coverage;
    private Double duplicatedLinesDensity;
}
