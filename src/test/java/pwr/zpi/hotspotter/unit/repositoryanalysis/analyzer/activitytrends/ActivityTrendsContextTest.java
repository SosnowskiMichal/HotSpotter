package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.activitytrends;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.ActivityTrendsContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrendsDailyStats;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ActivityTrendsContextTest {

    @Test
    void recordContribution_createsDailyStatsAndUpdatesValues() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024,1,5), 2);

        ctx.recordContribution(LocalDate.of(2024,1,1), "John", 10, 2);

        ActivityTrendsDailyStats stats = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024,1,1));
        assertThat(stats.getCommits()).isEqualTo(1);
        assertThat(stats.getLinesAdded()).isEqualTo(10);
        assertThat(stats.getLinesDeleted()).isEqualTo(2);

        assertThat(ctx.getUniqueAuthors()).contains("John");
        assertThat(ctx.getAuthorLastActivity().get("John")).isEqualTo(LocalDate.of(2024,1,1));
        assertThat(ctx.getUniqueAuthors().size()).isEqualTo(1);
    }

    @Test
    void recordContribution_aggregatesMissingDays() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024,1,10), 2);

        ctx.recordContribution(LocalDate.of(2024,1,1), "A", 5, 1);
        ctx.recordContribution(LocalDate.of(2024,1,4), "A", 3, 0);

        assertThat(ctx.getActivityTrendsDailyStats()).containsKeys(
                LocalDate.of(2024,1,1),
                LocalDate.of(2024,1,2),
                LocalDate.of(2024,1,3),
                LocalDate.of(2024,1,4)
        );

        ActivityTrendsDailyStats gapStats1 = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024,1,2));
        assertThat(gapStats1.getActiveAuthors()).isEqualTo(1);
        assertThat(gapStats1.getUniqueAuthors()).isEqualTo(0);

        ActivityTrendsDailyStats gapStats2 = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024,1,4));
        assertThat(gapStats2.getActiveAuthors()).isEqualTo(1);
        assertThat(gapStats2.getUniqueAuthors()).isEqualTo(1);

        ActivityTrendsDailyStats gapStats3 = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024,1,1));
        assertThat(gapStats3.getActiveAuthors()).isEqualTo(1);
        assertThat(gapStats3.getUniqueAuthors()).isEqualTo(1);

        ActivityTrendsDailyStats gapStats4 = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024,1,5));
        assertThat(gapStats4).isEqualTo(null);
    }

    @Test
    void removeInactiveAuthors_removesStaleEntries() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024,1,10), 1);

        ctx.recordContribution(LocalDate.of(2023,12,1), "A", 1, 1);

        ctx.recordContribution(LocalDate.of(2024,1,5), "B", 1, 1);

        assertThat(ctx.getAuthorLastActivity()).containsOnlyKeys("B");
    }

    @Test
    void finishAnalysis_fillsMissingDaysUpToReferenceDate() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024,1,5), 2);

        ctx.recordContribution(LocalDate.of(2024,1,1), "A", 5, 1);

        ctx.finishAnalysis();

        assertThat(ctx.getActivityTrendsDailyStats()).containsKeys(
                LocalDate.of(2024,1,1),
                LocalDate.of(2024,1,2),
                LocalDate.of(2024,1,3),
                LocalDate.of(2024,1,4)
        );
    }
}
