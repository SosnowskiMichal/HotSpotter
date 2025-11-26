package pwr.zpi.hotspotter.repositoryanalysis.dto;

public record FileCodeAgeDTO(
        String path,
        Integer codeAgeDays,
        Double normalizedValue
)
{ }
