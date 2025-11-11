package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends;

import lombok.Getter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrendsDailyStats;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

public class ActivityTrendsContext {

    @Getter
    private final String analysisId;
    private final int activeWindowDays;

    private final Map<LocalDate, Integer> commitsPerDate = new HashMap<>();
    private final Map<LocalDate, Set<String>> authorsPerDate = new HashMap<>();
    private final Map<LocalDate, Integer> linesAddedPerDate = new HashMap<>();
    private final Map<LocalDate, Integer> linesDeletedPerDate = new HashMap<>();

    @Getter
    private List<ActivityTrendsDailyStats> dailyStats = Collections.emptyList();

    public ActivityTrendsContext(String analysisId, int activeWindowDays) {
        this.analysisId = analysisId;
        this.activeWindowDays = activeWindowDays;
    }

    public void recordCommit(Commit commit) {
        LocalDate date = commit.getCommitDateAsLocalDate();
        String author = commit.author();

        commitsPerDate.merge(date, 1, Integer::sum);
        authorsPerDate.computeIfAbsent(date, _ -> new HashSet<>()).add(author);

        int added = 0;
        int deleted = 0;
        if (commit.changedFiles() != null) {
            for (FileChange fc : commit.changedFiles()) {
                added += fc.linesAdded();
                deleted += fc.linesDeleted();
            }
        }
        linesAddedPerDate.merge(date, added, Integer::sum);
        linesDeletedPerDate.merge(date, deleted, Integer::sum);
    }

    public void computeDailyStats() {
        if (commitsPerDate.isEmpty()) {
            dailyStats = Collections.emptyList();
            return;
        }

        LocalDate minDate = Stream.concat(commitsPerDate.keySet().stream(), authorsPerDate.keySet().stream())
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
        LocalDate maxDate = Stream.concat(commitsPerDate.keySet().stream(), authorsPerDate.keySet().stream())
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now());

        List<LocalDate> allDates = minDate.datesUntil(maxDate.plusDays(1)).toList();

        Map<LocalDate, Set<String>> authorsByDateAll = new HashMap<>();
        for (LocalDate d : allDates) {
            authorsByDateAll.put(d, authorsPerDate.getOrDefault(d, Collections.emptySet()));
        }

        List<ActivityTrendsDailyStats> stats = new ArrayList<>(allDates.size());
        for (LocalDate d : allDates) {
            int commits = commitsPerDate.getOrDefault(d, 0);
            int uniqueAuthors = authorsByDateAll.getOrDefault(d, Collections.emptySet()).size();

            LocalDate windowStart = d.minusDays(Math.max(0, activeWindowDays - 1));
            Set<String> activeAuthorsSet = new HashSet<>();
            for (LocalDate w = windowStart; !w.isAfter(d); w = w.plusDays(1)) {
                activeAuthorsSet.addAll(authorsByDateAll.getOrDefault(w, Collections.emptySet()));
            }
            int activeAuthors = activeAuthorsSet.size();

            int added = linesAddedPerDate.getOrDefault(d, 0);
            int deleted = linesDeletedPerDate.getOrDefault(d, 0);

            stats.add(ActivityTrendsDailyStats.builder()
                    .date(d)
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
