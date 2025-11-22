package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.util.List;

public record FileCouplingDTO(
        String path,
        List<CoupledFileDTO> coupledFiles
) {
    public record CoupledFileDTO(
        String path,
        Integer sharedCommits,
        Double percentage
    ) { }
}
