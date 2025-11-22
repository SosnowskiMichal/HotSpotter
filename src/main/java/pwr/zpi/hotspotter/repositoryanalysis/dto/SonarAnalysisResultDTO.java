package pwr.zpi.hotspotter.repositoryanalysis.dto;

public record SonarAnalysisResultDTO(
        Integer bugs,
        Integer vulnerabilities,
        Integer codeSmells,
        Integer complexity,
        Double duplicatedLinesDensity
) { }
