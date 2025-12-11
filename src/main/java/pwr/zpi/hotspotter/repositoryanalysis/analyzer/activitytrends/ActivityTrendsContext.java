package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends;

import lombok.Getter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrendsDailyStats;

import java.time.LocalDate;
import java.util.*;

@Getter
public class ActivityTrendsContext {

    private static final int AUTHOR_INACTIVITY_THRESHOLD_MONTHS = 6;

    private final String analysisId;
    private final LocalDate referenceDate;
    private final Map<LocalDate, ActivityTrendsDailyStats> activityTrendsDailyStats;

    private LocalDate firstCommitDate;
    private LocalDate lastCommitDate;
    private final Map<LocalDate, Set<String>> dailyUniqueAuthors;
    private final Map<String, LocalDate> authorLastActivity;

    public ActivityTrendsContext(String analysisId, LocalDate referenceDate) {
        this.analysisId = analysisId;
        this.referenceDate = referenceDate != null ? referenceDate : LocalDate.now();
        this.activityTrendsDailyStats = new LinkedHashMap<>();

        this.firstCommitDate = null;
        this.lastCommitDate = null;
        this.dailyUniqueAuthors = new HashMap<>();
        this.authorLastActivity = new HashMap<>();
    }

    public void recordContribution(LocalDate date, String author, int linesAdded, int linesDeleted) {
        if (firstCommitDate == null || date.isBefore(firstCommitDate)) {
            firstCommitDate = date;
        }
        if (lastCommitDate == null || date.isAfter(lastCommitDate)) {
            lastCommitDate = date;
        }

        activityTrendsDailyStats
                .compute(date, (_, dailyStats) -> {
                    if (dailyStats == null) {
                        dailyStats = ActivityTrendsDailyStats.builder()
                                .date(date)
                                .build();
                    }

                    dailyStats.incrementCommits();
                    dailyStats.increaseLinesAdded(linesAdded);
                    dailyStats.increaseLinesDeleted(linesDeleted);
                    return dailyStats;
                });

        dailyUniqueAuthors.computeIfAbsent(date, _ -> new HashSet<>()).add(author);

        authorLastActivity.compute(author, (_, lastActivity) -> {
            if (lastActivity == null || date.isAfter(lastActivity)) {
                return date;
            }
            return lastActivity;
        });
    }

    public void finishAnalysis() {
        if (firstCommitDate == null) return;

        Map<String, LocalDate> authorLastActivityUpToDate = new HashMap<>();

        LocalDate currentDate = firstCommitDate;

        while (!currentDate.isAfter(referenceDate)) {
            final LocalDate date = currentDate;

            Set<String> authorsForDay = dailyUniqueAuthors.getOrDefault(date, Set.of());
            for (String author : authorsForDay) {
                authorLastActivityUpToDate.put(author, date);
            }

            LocalDate inactivityThreshold = date.minusMonths(AUTHOR_INACTIVITY_THRESHOLD_MONTHS);
            long activeAuthorsCount = authorLastActivityUpToDate.entrySet().stream()
                    .filter(entry -> !entry.getValue().isBefore(inactivityThreshold))
                    .count();

            int uniqueAuthorsCount = authorsForDay.size();

            activityTrendsDailyStats.compute(date, (_, dailyStats) -> {
                if (dailyStats == null) {
                    dailyStats = ActivityTrendsDailyStats.builder()
                            .date(date)
                            .build();
                }
                dailyStats.setUniqueAuthors(uniqueAuthorsCount);
                dailyStats.setActiveAuthors((int) activeAuthorsCount);
                return dailyStats;
            });

            currentDate = currentDate.plusDays(1);
        }
    }

}
