package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.util.List;

public record AuthorCouplingDTO(
        String name,
        Integer filesChanged,
        Integer totalChanges,
        List<CoupledAuthorDTO> coupledAuthors
) {
    public record CoupledAuthorDTO(
            String name,
            Integer sharedFilesChanged,
            Integer sharedChanges,
            Double percentage

    ) { }
}
