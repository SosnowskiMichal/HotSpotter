package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnalysisHistoryEntryDTO(
        String id,
        String repositoryUrl,
        LocalDateTime startedAt,
        LocalDate startDate,
        LocalDate endDate,
        String status
) { }
