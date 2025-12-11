package pwr.zpi.hotspotter.fileanalysis.methods.model;

import java.time.LocalDate;

public record MethodVersion(
        String commitHash,
        LocalDate commitDate,
        String methodName,
        Integer startLine,
        Integer endLine,
        Integer complexity,
        boolean wasTouched
) { }
