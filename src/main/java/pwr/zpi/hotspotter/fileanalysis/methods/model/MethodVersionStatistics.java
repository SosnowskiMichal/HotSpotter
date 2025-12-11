package pwr.zpi.hotspotter.fileanalysis.methods.model;

import java.time.LocalDate;

public record MethodVersionStatistics(
        LocalDate date,
        Integer complexity,
        Integer lines
) { }
