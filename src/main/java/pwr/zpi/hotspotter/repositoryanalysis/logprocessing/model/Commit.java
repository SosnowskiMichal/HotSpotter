package pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model;

import java.util.List;

public record Commit(
        String hash,
        String date,
        String author,
        String email,
        List<FileChange> changedFiles
) { }
