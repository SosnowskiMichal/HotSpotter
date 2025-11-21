package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.activitytrends;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.ActivityTrendsAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.ActivityTrendsContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrends;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrendsDailyStats;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.repository.ActivityTrendsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityTrendsAnalyzerTest {

    @Mock
    private ActivityTrendsRepository repository;

    @InjectMocks
    private ActivityTrendsAnalyzer analyzer;

    @Test
    void startAnalysis_createsContext() {
        LocalDate ref = LocalDate.of(2024, 1, 1);

        ActivityTrendsContext ctx = analyzer.startAnalysis("A1", ref, 3);

        assertThat(ctx.getAnalysisId()).isEqualTo("A1");
        assertThat(ctx.getReferenceDate()).isEqualTo(ref);
        assertThat(ctx.getAuthorInactivityThresholdMonths()).isEqualTo(3);
        assertThat(ctx.getActivityTrendsDailyStats()).isEmpty();
    }

    @Test
    void processCommit_addsStatsToContext() {
        ActivityTrendsContext ctx = analyzer.startAnalysis("A1", LocalDate.of(2024,1,10), 2);

        Commit commit = new Commit(
                "id1",
                LocalDate.of(2024, 1, 5).toString(),
                "author1",
                "author@email.com",
                List.of(
                        new FileChange("A.java", 10, 2),
                        new FileChange("B.java", 3, 1)
                )
        );

        analyzer.processCommit(commit, ctx);

        ActivityTrendsDailyStats stats = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024,1,5));

        assertThat(stats.getCommits()).isEqualTo(1);
        assertThat(stats.getLinesAdded()).isEqualTo(13);
        assertThat(stats.getLinesDeleted()).isEqualTo(3);
    }

    @Test
    void finishAnalysis_savesActivityTrends() {
        ActivityTrendsContext ctx = analyzer.startAnalysis("A1", LocalDate.of(2024,1,5), 1);

        ctx.recordContribution(LocalDate.of(2024,1,1), "A", 5, 1);
        ctx.recordContribution(LocalDate.of(2024,1,2), "A", 3, 0);

        analyzer.finishAnalysis(ctx);

        ArgumentCaptor<ActivityTrends> captor = ArgumentCaptor.forClass(ActivityTrends.class);
        verify(repository).save(captor.capture());

        ActivityTrends saved = captor.getValue();

        assertThat(saved.getAnalysisId()).isEqualTo("A1");
        assertThat(saved.getDailyStats()).hasSize(5);
    }

    @Test
    void finishAnalysis_handlesExceptionGracefully() {
        ActivityTrendsContext ctx = analyzer.startAnalysis("A1", LocalDate.now(), 1);

        doThrow(new RuntimeException("fail")).when(repository).save(any());

        analyzer.finishAnalysis(ctx);

        verify(repository).save(any());
    }
}