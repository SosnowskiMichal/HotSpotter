package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends;

import lombok.Getter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrendsDailyStats;

import java.time.LocalDate;
import java.util.*;

@Getter
public class ActivityTrendsContext {

    private final String analysisId;
    private final LocalDate referenceDate;
    private final int authorInactivityThresholdMonths;
    private final Map<LocalDate, ActivityTrendsDailyStats> activityTrendsDailyStats;

    private LocalDate lastDate;
    private final Set<String> uniqueAuthors;
    private final Map<String, LocalDate> authorLastActivity;

    public ActivityTrendsContext(String analysisId, LocalDate referenceDate, int authorInactivityThresholdMonths) {
        this.analysisId = analysisId;
        this.referenceDate = referenceDate != null ? referenceDate : LocalDate.now();
        this.authorInactivityThresholdMonths = authorInactivityThresholdMonths;
        this.activityTrendsDailyStats = new LinkedHashMap<>();

        this.lastDate = null;
        this.uniqueAuthors = new HashSet<>();
        this.authorLastActivity = new HashMap<>();
    }

    public void recordContribution(LocalDate date, String author, int linesAdded, int linesDeleted) {
        if (lastDate != null && date.isAfter(lastDate)) {
            aggregateStatsForDaysBetween(lastDate, date);
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

        uniqueAuthors.add(author);
        authorLastActivity.put(author, date);
        lastDate = date;
    }

    public void finishAnalysis() {
        aggregateStatsForDaysBetween(lastDate, referenceDate);
    }

    private void aggregateStatsForDaysBetween(LocalDate startDate, LocalDate endDate) {
        LocalDate date = startDate;

        while (date.isBefore(endDate)) {
            final LocalDate currentDate = date;
            removeInactiveAuthors(currentDate);

            int activeAuthorsCount = authorLastActivity.size();
            int uniqueAuthorsCount = uniqueAuthors.size();

            activityTrendsDailyStats.compute(date, (_, dailyStats) -> {
                if (dailyStats == null) {
                    dailyStats = ActivityTrendsDailyStats.builder()
                            .date(currentDate)
                            .build();
                }

                dailyStats.setUniqueAuthors(uniqueAuthorsCount);
                dailyStats.setActiveAuthors(activeAuthorsCount);
                return dailyStats;
            });

            uniqueAuthors.clear();
            date = date.plusDays(1);
        }
    }

    private void removeInactiveAuthors(LocalDate date) {
        LocalDate inactivityThreshold = date.minusMonths(authorInactivityThresholdMonths);
        authorLastActivity.entrySet().removeIf(entry ->
                entry.getValue().isBefore(inactivityThreshold)
        );
    }

}
