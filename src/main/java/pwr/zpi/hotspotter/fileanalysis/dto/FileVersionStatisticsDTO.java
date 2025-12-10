package pwr.zpi.hotspotter.fileanalysis.dto;

import java.time.LocalDate;

public record FileVersionStatisticsDTO(
        LocalDate date,
        String url,
        Integer totalLines,
        Integer codeLines,
        Integer commentLines,
        Integer blankLines,
        Integer complexity,
        Integer methods
) { }
