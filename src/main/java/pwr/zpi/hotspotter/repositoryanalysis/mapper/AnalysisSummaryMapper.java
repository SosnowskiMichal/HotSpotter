package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.AnalysisStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisInfoDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisSummaryDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisStatisticsDTO;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;

@Component
@RequiredArgsConstructor
public class AnalysisSummaryMapper {

    private final AnalysisInfoMapper analysisInfoMapper;
    private final AnalysisStatisticsMapper analysisStatisticsMapper;

    public AnalysisSummaryDTO toDTO(AnalysisInfo analysisInfo, AnalysisStatistics analysisStatistics) {
        AnalysisInfoDTO analysisInfoDTO = analysisInfoMapper.toDTO(analysisInfo);
        AnalysisStatisticsDTO analysisStatisticsDTO = analysisStatisticsMapper.toDTO(analysisStatistics);

        return new AnalysisSummaryDTO(analysisInfoDTO, analysisStatisticsDTO);
    }

}
