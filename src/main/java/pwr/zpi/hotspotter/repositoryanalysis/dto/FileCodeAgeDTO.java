package pwr.zpi.hotspotter.repositoryanalysis.dto;

public record FileCodeAgeDTO(
        String path,
        String name,
        Integer codeAgeDays,
        Double normalizedValue
)
{ }
