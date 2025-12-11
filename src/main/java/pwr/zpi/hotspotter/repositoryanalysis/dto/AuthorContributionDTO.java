package pwr.zpi.hotspotter.repositoryanalysis.dto;

public record AuthorContributionDTO(
        String name,
        Integer linesAdded,
        Integer commits,
        Double percentage
) { }
