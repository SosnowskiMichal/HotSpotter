package pwr.zpi.hotspotter.repositoryanalysis.dto;

public record SonarAnalysisComponentDTO(
        Integer bugs,
        Integer vulnerabilities,
        Integer codeSmells,
        Integer complexity,
        Double testCoverage,
        Double duplicatedLinesDensity
) { }
