package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnalysisInfoDTO(
        String id,
        String repositoryUrl,
        String repositoryName,
        String repositoryOwner,
        String repositoryPlatform,
        LocalDate analysisRangeStartDate,
        LocalDate analysisRangeEndDate,
        LocalDateTime analysisStartedAt,
        LocalDateTime analysisFinishedAt,
        Long analysisTimeInSeconds
) { }
