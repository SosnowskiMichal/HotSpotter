package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.dto.SonarAnalysisComponentDTO;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;

@Component
public class SonarAnalysisComponentMapper {

    public SonarAnalysisComponentDTO toDTO(SonarRepoAnalysisComponent component) {
        if (component == null) return null;

        return new SonarAnalysisComponentDTO(
                component.getBugs(),
                component.getVulnerabilities(),
                component.getCodeSmells(),
                component.getComplexity(),
                component.getCoverage(),
                component.getDuplicatedLinesDensity()
        );
    }

}
