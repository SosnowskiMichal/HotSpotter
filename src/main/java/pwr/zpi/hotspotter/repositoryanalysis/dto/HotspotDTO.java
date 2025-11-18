package pwr.zpi.hotspotter.repositoryanalysis.dto;

public record HotspotDTO(
        String path,
        String name,
        Integer commitsInHotspotAnalysisPeriod,
        Integer codeLines,
        Double normalizedValue
) { }
