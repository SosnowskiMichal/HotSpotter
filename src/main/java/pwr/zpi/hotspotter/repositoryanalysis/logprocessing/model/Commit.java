package pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record Commit(
        String hash,
        String date,
        String author,
        String email,
        List<FileChange> changedFiles
) {
    public LocalDate getCommitDateAsLocalDate() {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
