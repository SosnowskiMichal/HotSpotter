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
    void allGetters_areCoveredForLombok() {
        LocalDate refDate = LocalDate.of(2024, 1, 5);
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", refDate, 2);

        assertThat(ctx.getAnalysisId()).isEqualTo("A1");
        assertThat(ctx.getReferenceDate()).isEqualTo(refDate);
        assertThat(ctx.getAuthorInactivityThresholdMonths()).isEqualTo(2);
        assertThat(ctx.getLastDate()).isNull();
        assertThat(ctx.getFirstCommitDate()).isNull();
        assertThat(ctx.getActivityTrendsDailyStats()).isNotNull();
        assertThat(ctx.getUniqueAuthors()).isNotNull();
        assertThat(ctx.getAuthorLastActivity()).isNotNull();
    }

    @Test
    void constructor_withValidReferenceDate_setsAllFields() {
        LocalDate refDate = LocalDate.of(2024, 1, 5);
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", refDate, 2);

        assertThat(ctx.getAnalysisId()).isEqualTo("A1");
        assertThat(ctx.getReferenceDate()).isEqualTo(refDate);
        assertThat(ctx.getAuthorInactivityThresholdMonths()).isEqualTo(2);
        assertThat(ctx.getLastDate()).isNull();
        assertThat(ctx.getFirstCommitDate()).isNull();
        assertThat(ctx.getActivityTrendsDailyStats()).isEmpty();
        assertThat(ctx.getUniqueAuthors()).isEmpty();
        assertThat(ctx.getAuthorLastActivity()).isEmpty();
    }

    @Test
    void constructor_withNullReferenceDate_usesCurrentDate() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", null, 2);

        assertThat(ctx.getReferenceDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void recordContribution_firstContribution_createsStatsWithoutAggregation() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 5), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "John", 10, 2);

        ActivityTrendsDailyStats stats = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 1));
        assertThat(stats.getCommits()).isEqualTo(1);
        assertThat(stats.getLinesAdded()).isEqualTo(10);
        assertThat(stats.getLinesDeleted()).isEqualTo(2);
        assertThat(ctx.getLastDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(ctx.getUniqueAuthors()).containsExactly("John");
        assertThat(ctx.getAuthorLastActivity()).containsEntry("John", LocalDate.of(2024, 1, 1));
    }

    @Test
    void recordContribution_sameDayMultipleContributions_aggregatesWithoutGapFilling() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 5), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "John", 10, 2);
        ctx.recordContribution(LocalDate.of(2024, 1, 1), "Jane", 5, 3);

        ActivityTrendsDailyStats stats = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 1));
        assertThat(stats.getCommits()).isEqualTo(2);
        assertThat(stats.getLinesAdded()).isEqualTo(15);
        assertThat(stats.getLinesDeleted()).isEqualTo(5);
        assertThat(ctx.getActivityTrendsDailyStats()).hasSize(1);
    }

    @Test
    void recordContribution_earlierDate_doesNotTriggerAggregation() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 10), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 5), "A", 5, 1);
        ctx.recordContribution(LocalDate.of(2024, 1, 3), "B", 3, 2);

        assertThat(ctx.getActivityTrendsDailyStats()).hasSize(2);
        assertThat(ctx.getActivityTrendsDailyStats()).containsOnlyKeys(
                LocalDate.of(2024, 1, 5),
                LocalDate.of(2024, 1, 3)
        );
    }

    @Test
    void recordContribution_laterDate_triggersAggregation() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 10), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "A", 5, 1);
        ctx.recordContribution(LocalDate.of(2024, 1, 4), "B", 3, 0);

        assertThat(ctx.getActivityTrendsDailyStats()).containsKeys(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 3),
                LocalDate.of(2024, 1, 4)
        );
    }

    @Test
    void finishAnalysis_fillsMissingDaysUpToReferenceDate() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 5), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "A", 5, 1);
        ctx.finishAnalysis();

        assertThat(ctx.getActivityTrendsDailyStats()).containsKeys(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 2),
                LocalDate.of(2024, 1, 3),
                LocalDate.of(2024, 1, 4),
                LocalDate.of(2024, 1, 5)
        );
    }

    @Test
    void finishAnalysis_whenLastDateAfterReferenceDate_loopNotEntered() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 1), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 5), "A", 5, 1);
        ctx.finishAnalysis();

        assertThat(ctx.getActivityTrendsDailyStats()).hasSize(1);
    }

    @Test
    void aggregateStats_existingDailyStats_updatesAuthorCounts() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 5), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "A", 5, 1);
        ctx.recordContribution(LocalDate.of(2024, 1, 3), "B", 3, 0);

        ctx.finishAnalysis();

        ActivityTrendsDailyStats jan1Stats = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 1));
        assertThat(jan1Stats.getCommits()).isEqualTo(1);
        assertThat(jan1Stats.getUniqueAuthors()).isEqualTo(1);
        assertThat(jan1Stats.getActiveAuthors()).isEqualTo(1);
    }

    @Test
    void aggregateStats_gapDays_createsNewDailyStats() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 5), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "A", 5, 1);
        ctx.recordContribution(LocalDate.of(2024, 1, 4), "A", 3, 0);

        ctx.finishAnalysis();

        ActivityTrendsDailyStats jan2Stats = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 2));
        assertThat(jan2Stats.getCommits()).isEqualTo(0);
        assertThat(jan2Stats.getUniqueAuthors()).isEqualTo(0);
        assertThat(jan2Stats.getActiveAuthors()).isEqualTo(1);
    }

    @Test
    void aggregateStats_clearsUniqueAuthorsAfterEachDay() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 5), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "A", 5, 1);
        ctx.recordContribution(LocalDate.of(2024, 1, 1), "B", 3, 1);
        ctx.recordContribution(LocalDate.of(2024, 1, 3), "C", 2, 0);

        ctx.finishAnalysis();

        assertThat(ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 1)).getUniqueAuthors()).isEqualTo(2);

        assertThat(ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 2)).getUniqueAuthors()).isEqualTo(0);

        assertThat(ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 3)).getUniqueAuthors()).isEqualTo(1);
    }

    @Test
    void removeInactiveAuthors_removesStaleEntries() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 2, 10), 1);

        ctx.recordContribution(LocalDate.of(2023, 12, 1), "A", 1, 1);
        ctx.recordContribution(LocalDate.of(2024, 2, 5), "B", 1, 1);

        assertThat(ctx.getAuthorLastActivity()).containsOnlyKeys("B");
    }

    @Test
    void removeInactiveAuthors_keepsActiveAuthors() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 10), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "A", 1, 1);
        ctx.recordContribution(LocalDate.of(2024, 1, 2), "B", 1, 1);

        ctx.finishAnalysis();

        ActivityTrendsDailyStats jan2Stats = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 2));
        assertThat(jan2Stats.getActiveAuthors()).isEqualTo(2);
    }

    @Test
    void removeInactiveAuthors_authorBecomesInactiveOverTime() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 3, 15), 1);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "A", 1, 1);
        ctx.recordContribution(LocalDate.of(2024, 3, 1), "B", 1, 1);

        ctx.finishAnalysis();

        ActivityTrendsDailyStats mar1Stats = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 3, 1));
        assertThat(mar1Stats.getActiveAuthors()).isEqualTo(1);
    }

    @Test
    void sameAuthorMultipleContributionsSameDay_countedOnce() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 5), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "John", 10, 2);
        ctx.recordContribution(LocalDate.of(2024, 1, 1), "John", 5, 1);

        assertThat(ctx.getUniqueAuthors()).hasSize(1);
        assertThat(ctx.getAuthorLastActivity()).hasSize(1);
    }

    @Test
    void authorActivityUpdated_onNewContribution() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 10), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "John", 10, 2);
        ctx.recordContribution(LocalDate.of(2024, 1, 5), "John", 5, 1);

        assertThat(ctx.getAuthorLastActivity().get("John")).isEqualTo(LocalDate.of(2024, 1, 5));
    }

    @Test
    void fullWorkflow_multipleAuthorsAndDays() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("analysis-123", LocalDate.of(2024, 1, 10), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 1), "Alice", 100, 10);
        ctx.recordContribution(LocalDate.of(2024, 1, 1), "Bob", 50, 5);
        ctx.recordContribution(LocalDate.of(2024, 1, 5), "Alice", 30, 3);
        ctx.recordContribution(LocalDate.of(2024, 1, 5), "Charlie", 20, 2);

        ctx.finishAnalysis();

        assertThat(ctx.getActivityTrendsDailyStats()).hasSize(10);

        ActivityTrendsDailyStats jan1 = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 1));
        assertThat(jan1.getCommits()).isEqualTo(2);
        assertThat(jan1.getLinesAdded()).isEqualTo(150);
        assertThat(jan1.getLinesDeleted()).isEqualTo(15);
        assertThat(jan1.getUniqueAuthors()).isEqualTo(2);

        ActivityTrendsDailyStats jan5 = ctx.getActivityTrendsDailyStats().get(LocalDate.of(2024, 1, 5));
        assertThat(jan5.getCommits()).isEqualTo(2);
        assertThat(jan5.getUniqueAuthors()).isEqualTo(2);
    }

    @Test
    void getFirstCommitDate_emptyContext_returnsNull() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 10), 2);

        assertThat(ctx.getFirstCommitDate()).isNull();
    }

    @Test
    void getFirstCommitDate_singleContribution_returnsDate() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 10), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 5), "Alice", 10, 2);

        assertThat(ctx.getFirstCommitDate()).isEqualTo(LocalDate.of(2024, 1, 5));
    }

    @Test
    void getFirstCommitDate_afterFinishAnalysis_remainsConsistent() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 10), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 3), "Alice", 10, 2);
        LocalDate firstDateBeforeFinish = ctx.getFirstCommitDate();

        ctx.finishAnalysis();
        LocalDate firstDateAfterFinish = ctx.getFirstCommitDate();

        assertThat(firstDateBeforeFinish).isEqualTo(LocalDate.of(2024, 1, 3));
        assertThat(firstDateAfterFinish).isEqualTo(LocalDate.of(2024, 1, 3));
    }

    @Test
    void getFirstCommitDate_chronologicalContributions_returnsEarliestDate() {
        ActivityTrendsContext ctx = new ActivityTrendsContext("A1", LocalDate.of(2024, 1, 10), 2);

        ctx.recordContribution(LocalDate.of(2024, 1, 3), "Alice", 10, 2);
        ctx.recordContribution(LocalDate.of(2024, 1, 5), "Bob", 5, 1);
        ctx.recordContribution(LocalDate.of(2024, 1, 7), "Charlie", 8, 3);

        assertThat(ctx.getFirstCommitDate()).isEqualTo(LocalDate.of(2024, 1, 3));
    }

}