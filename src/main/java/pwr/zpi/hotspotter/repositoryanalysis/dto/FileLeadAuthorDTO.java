package pwr.zpi.hotspotter.repositoryanalysis.dto;

public record FileLeadAuthorDTO(
        String path,
        String name,
        String leadAuthor,
        Double percentage
) { }
