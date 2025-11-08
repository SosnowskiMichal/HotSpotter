package pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors;

import lombok.Getter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Getter
public class AuthorsAnalyzerContext {

    private final String analysisId;
    private final Map<String, AuthorStatistics> authorStatistics;

    public AuthorsAnalyzerContext(String analysisId) {
        this.analysisId = analysisId;
        this.authorStatistics = new HashMap<>();
    }

    public void recordContribution(String name, String email, LocalDate date, int linesAdded, int linesDeleted) {
        authorStatistics
                .compute(name, (_, stats) -> {
                    if (stats == null) {
                        LocalDate currentDate = LocalDate.now();
                        int daysSinceFirstCommit = (int) ChronoUnit.DAYS.between(date, currentDate);
                        int monthsSinceFirstCommit = (int) ChronoUnit.MONTHS.between(date, currentDate);

                        stats = AuthorStatistics.builder()
                                .analysisId(analysisId)
                                .name(name)
                                .firstCommitDate(date)
                                .lastCommitDate(date)
                                .daysSinceFirstCommit(daysSinceFirstCommit)
                                .monthsSinceFirstCommit(monthsSinceFirstCommit)
                                .build();
                    }
                    stats.addEmail(email);
                    stats.setLastCommitDate(date);
                    stats.increaseLinesAdded(linesAdded);
                    stats.increaseLinesDeleted(linesDeleted);
                    stats.incrementCommits();
                    return stats;
                });
    }

}
