package pwr.zpi.hotspotter.fileanalysis.dto;

import java.time.LocalDate;

public record FileCommitDTO(
        String hash,
        LocalDate date,
        String author,
        Integer linesAdded,
        Integer linesDeleted
) { }
