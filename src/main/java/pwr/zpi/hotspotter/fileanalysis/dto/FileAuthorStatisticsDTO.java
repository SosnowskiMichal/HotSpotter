package pwr.zpi.hotspotter.fileanalysis.dto;

public record FileAuthorStatisticsDTO(
        String name,
        Integer linesAdded,
        Double percentage
) { }
