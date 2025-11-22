package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.util.Set;

public record AuthorSummaryDTO(
        String name,
        Set<String> emails,
        boolean isActive
) { }
