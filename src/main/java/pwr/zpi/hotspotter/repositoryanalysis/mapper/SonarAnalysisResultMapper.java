package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.dto.SonarAnalysisResultDTO;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;

@Component
public class SonarAnalysisResultMapper {

    public SonarAnalysisResultDTO toDTO(SonarRepoAnalysisComponent component) {
        if (component == null) return null;

        return new SonarAnalysisResultDTO(
                component.getBugs(),
                component.getVulnerabilities(),
                component.getCodeSmells(),
                component.getComplexity(),
                component.getDuplicatedLinesDensity()
        );
    }

    public SonarAnalysisResultDTO toDTO(SonarRepoAnalysisResult result) {
        if (result == null) return null;

        return new SonarAnalysisResultDTO(
                result.getBugs(),
                result.getVulnerabilities(),
                result.getCodeSmells(),
                result.getComplexity(),
                result.getDuplicatedLinesDensity()
        );
    }

}
