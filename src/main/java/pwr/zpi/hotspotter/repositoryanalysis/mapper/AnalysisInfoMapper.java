package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisInfoDTO;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;

@Component
public class AnalysisInfoMapper {

    public AnalysisInfoDTO toDTO(AnalysisInfo analysisInfo) {
        if (analysisInfo == null) return null;

        return new AnalysisInfoDTO(
                analysisInfo.getId(),
                analysisInfo.getRepositoryUrl(),
                analysisInfo.getRepositoryName(),
                analysisInfo.getRepositoryOwner(),
                analysisInfo.getRepositoryPlatform(),
                analysisInfo.getStartDate(),
                analysisInfo.getEndDate(),
                analysisInfo.getAnalysisStartedAt(),
                analysisInfo.getAnalysisFinishedAt(),
                analysisInfo.getAnalysisTimeInSeconds()
        );
    }

}
