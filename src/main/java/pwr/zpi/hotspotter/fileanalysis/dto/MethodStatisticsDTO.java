package pwr.zpi.hotspotter.fileanalysis.dto;

import java.time.LocalDate;
import java.util.List;

public record MethodStatisticsDTO(
        String name,
        Integer startLine,
        Integer endLine,
        Integer lines,
        String url,
        Integer commits,
        Integer authors,
        LocalDate firstCommitDate,
        LocalDate lastCommitDate,
        Integer daysSinceLastCommit,
        List<MethodVersionStatisticsDTO> complexityTrends
) {
    public record MethodVersionStatisticsDTO(
            LocalDate date,
            Integer complexity,
            Integer lines
    ) { }
}
