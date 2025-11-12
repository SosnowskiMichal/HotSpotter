package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrends;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.repository.ActivityTrendsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityTrendsAnalyzer {

    private final ActivityTrendsRepository activityTrendsRepository;

    public ActivityTrendsContext startAnalysis(String analysisId, int activeWindowMonths) {
        return new ActivityTrendsContext(analysisId, activeWindowMonths);
    }

    public void processCommit(Commit commit, ActivityTrendsContext context) {
        context.recordCommit(commit);
    }

    public void finishAnalysis(ActivityTrendsContext context) {
        context.computeDailyStats();
        try {
            activityTrendsRepository.save(ActivityTrends.builder()
                    .analysisId(context.getAnalysisId())
                    .dailyStats(context.getDailyStats())
                    .build());
            log.info("Computed {} daily activity stats for analysis {}", context.getDailyStats().size(), context.getAnalysisId());
        } catch (Exception e) {
            log.error("Failed to save activity trends stats for analysis {}: {}", context.getAnalysisId(), e.getMessage());
        }
    }
}
