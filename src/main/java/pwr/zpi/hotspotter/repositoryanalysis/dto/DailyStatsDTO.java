package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.time.LocalDate;

public record DailyStatsDTO(
        LocalDate date,
        Integer commits,
        Integer uniqueAuthors,
        Integer activeAuthors,
        Integer linesAdded,
        Integer linesDeleted
) { }
