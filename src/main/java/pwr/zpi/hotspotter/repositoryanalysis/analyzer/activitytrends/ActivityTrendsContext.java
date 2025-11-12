package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends;

import lombok.Getter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrendsDailyStats;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.time.LocalDate;
import java.util.*;

public class ActivityTrendsContext {

    @Getter
    private final String analysisId;
    private final int activeWindowMonths;

    private final Map<LocalDate, Integer> commitsPerDate = new HashMap<>();
    private final Map<LocalDate, Set<String>> authorsPerDate = new HashMap<>();
    private final Map<LocalDate, Integer> linesAddedPerDate = new HashMap<>();
    private final Map<LocalDate, Integer> linesDeletedPerDate = new HashMap<>();

    @Getter
    private List<ActivityTrendsDailyStats> dailyStats = Collections.emptyList();

    public ActivityTrendsContext(String analysisId, int activeWindowMonths) {
        this.analysisId = analysisId;
        this.activeWindowMonths = activeWindowMonths;
    }

    public void recordCommit(Commit commit) {
        LocalDate date = commit.getCommitDateAsLocalDate();
        String author = commit.author();

        commitsPerDate.merge(date, 1, Integer::sum);
        authorsPerDate.computeIfAbsent(date, _ -> new HashSet<>()).add(author);

        int totalLinesAdded = 0;
        int totalLinesDeleted = 0;
        if (commit.changedFiles() != null) {
            for (FileChange fileChange : commit.changedFiles()) {
                totalLinesAdded += fileChange.linesAdded();
                totalLinesDeleted += fileChange.linesDeleted();
            }
        }
        linesAddedPerDate.merge(date, totalLinesAdded, Integer::sum);
        linesDeletedPerDate.merge(date, totalLinesDeleted, Integer::sum);
    }

    public void computeDailyStats() {
        if (commitsPerDate.isEmpty()) {
            dailyStats = Collections.emptyList();
            return;
        }

        Set<LocalDate> dates = commitsPerDate.keySet();

        LocalDate minDate = Collections.min(dates);
        LocalDate maxDate = Collections.max(dates);

        List<LocalDate> allDates = minDate.datesUntil(maxDate.plusDays(1)).toList();

        Map<LocalDate, Set<String>> authorsByDateAll = new HashMap<>();
        for (LocalDate date : allDates) {
            authorsByDateAll.put(date, authorsPerDate.getOrDefault(date, Collections.emptySet()));
        }

        Map<LocalDate, Integer> activeAuthorsChangePerDate = new HashMap<>();
        for (LocalDate date : allDates) {
            Set<String> authors = authorsByDateAll.get(date);
            activeAuthorsChangePerDate.merge(date, authors.size(), Integer::sum);
            LocalDate endDate = date.plusMonths(activeWindowMonths);
            activeAuthorsChangePerDate.merge(endDate, -authors.size(), Integer::sum);
        }

        int activeAuthors = 0;
        List<ActivityTrendsDailyStats> stats = new ArrayList<>(allDates.size());
        for (LocalDate date : allDates) {
            int commits = commitsPerDate.getOrDefault(date, 0);
            int uniqueAuthors = authorsByDateAll.getOrDefault(date, Collections.emptySet()).size();

            int added = linesAddedPerDate.getOrDefault(date, 0);
            int deleted = linesDeletedPerDate.getOrDefault(date, 0);
            activeAuthors += activeAuthorsChangePerDate.getOrDefault(date, 0);

            stats.add(ActivityTrendsDailyStats.builder()
                    .date(date)
                    .commitsCount(commits)
                    .uniqueAuthorsCount(uniqueAuthors)
                    .activeAuthorsCount(activeAuthors)
                    .linesAdded(added)
                    .linesDeleted(deleted)
                    .build());
        }

        this.dailyStats = stats;
    }
}
