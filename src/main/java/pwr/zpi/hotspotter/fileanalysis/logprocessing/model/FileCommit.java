package pwr.zpi.hotspotter.fileanalysis.logprocessing.model;

import java.time.LocalDate;

public record FileCommit(
        String hash,
        LocalDate date,
        String author,
        String email,
        Integer linesAdded,
        Integer linesDeleted
) { }
