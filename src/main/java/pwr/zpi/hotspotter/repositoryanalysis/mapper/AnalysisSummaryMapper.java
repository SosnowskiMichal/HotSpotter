package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.AnalysisStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisInfoDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisSummaryDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisStatisticsDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.SonarAnalysisResultDTO;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;

@Component
@RequiredArgsConstructor
public class AnalysisSummaryMapper {

    private final AnalysisInfoMapper analysisInfoMapper;
    private final AnalysisStatisticsMapper analysisStatisticsMapper;
    private final SonarAnalysisResultMapper sonarAnalysisResultMapper;

    public AnalysisSummaryDTO toDTO(
            AnalysisInfo analysisInfo,
            AnalysisStatistics analysisStatistics,
            SonarRepoAnalysisResult sonarAnalysisResult
    ) {
        AnalysisInfoDTO analysisInfoDTO = analysisInfoMapper.toDTO(analysisInfo);
        AnalysisStatisticsDTO analysisStatisticsDTO = analysisStatisticsMapper.toDTO(analysisStatistics);
        SonarAnalysisResultDTO sonarAnalysisResultDTO = sonarAnalysisResultMapper.toDTO(sonarAnalysisResult);

        return new AnalysisSummaryDTO(analysisInfoDTO, analysisStatisticsDTO, sonarAnalysisResultDTO);
    }

}
