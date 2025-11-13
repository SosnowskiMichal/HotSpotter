package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrends;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrendsDailyStats;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.repository.ActivityTrendsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityTrendsAnalyzer {

    private final ActivityTrendsRepository activityTrendsRepository;

    public ActivityTrendsContext startAnalysis(String analysisId, LocalDate referenceDate, int authorInactivityThresholdMonths) {
        log.debug("Starting activity trends analysis for ID {}", analysisId);
        return new ActivityTrendsContext(analysisId, referenceDate, authorInactivityThresholdMonths);
    }

    public void processCommit(Commit commit, ActivityTrendsContext context) {
        if (commit == null || context == null) return;

        LocalDate date = commit.getCommitDateAsLocalDate();
        String author = commit.author();

        int linesAdded = commit.changedFiles().stream()
                .mapToInt(FileChange::linesAdded)
                .sum();
        int linesDeleted = commit.changedFiles().stream()
                .mapToInt(FileChange::linesDeleted)
                .sum();

        context.recordContribution(date, author, linesAdded, linesDeleted);
    }

    public void finishAnalysis(ActivityTrendsContext context) {
        if (context == null) return;

        context.finishAnalysis();

        List<ActivityTrendsDailyStats> dailyStats = context.getActivityTrendsDailyStats().values().stream().toList();
        ActivityTrends activityTrends = ActivityTrends.builder()
                .analysisId(context.getAnalysisId())
                .dailyStats(dailyStats)
                .build();

        try {
            activityTrendsRepository.save(activityTrends);
        } catch (Exception e) {
            log.error("Error saving activity trends data for ID {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

}
