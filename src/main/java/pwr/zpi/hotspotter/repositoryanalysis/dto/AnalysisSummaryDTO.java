package pwr.zpi.hotspotter.repositoryanalysis.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalysisSummaryDTO(
        AnalysisInfoDTO info,
        AnalysisStatisticsDTO statistics
) { }
