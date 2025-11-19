package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.authors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthorsAnalyzerContextTest {

    @Test
    void recordContribution_createsNewStats() {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        AuthorsAnalyzerContext ctx = new AuthorsAnalyzerContext("A1", ref);

        ctx.recordContribution("John", "john@mail", LocalDate.of(2024,1,10), 10, 2);

        AuthorStatistics stats = ctx.getAuthorStatistics().get("John");

        assertThat(stats.getAnalysisId()).isEqualTo("A1");
        assertThat(stats.getFirstCommitDate()).isEqualTo(LocalDate.of(2024,1,10));
        assertThat(stats.getLastCommitDate()).isEqualTo(LocalDate.of(2024,1,10));
        assertThat(stats.getTotalLinesAdded()).isEqualTo(10);
        assertThat(stats.getTotalLinesDeleted()).isEqualTo(2);
        assertThat(stats.getEmails()).contains("john@mail");

        assertThat(stats.getDaysSinceFirstCommit()).isEqualTo(10);
        assertThat(stats.getMonthsSinceFirstCommit()).isEqualTo(0);
    }

    @Test
    void recordContribution_accumulatesCommitData() {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        AuthorsAnalyzerContext ctx = new AuthorsAnalyzerContext("A1", ref);

        ctx.recordContribution("John", "a@mail", LocalDate.of(2024,1,10), 5, 1);
        ctx.recordContribution("John", "b@mail", LocalDate.of(2024,1,15), 3, 2);

        AuthorStatistics stats = ctx.getAuthorStatistics().get("John");

        assertThat(stats.getCommits()).isEqualTo(2);
        assertThat(stats.getTotalLinesAdded()).isEqualTo(8);
        assertThat(stats.getTotalLinesDeleted()).isEqualTo(3);
        assertThat(stats.getEmails()).containsExactlyInAnyOrder("a@mail", "b@mail");
        assertThat(stats.getLastCommitDate()).isEqualTo(LocalDate.of(2024,1,15));
    }

    @Test
    void recordContribution_newerCommitUpdatesLastCommitOnly() {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        AuthorsAnalyzerContext ctx = new AuthorsAnalyzerContext("A1", ref);

        ctx.recordContribution("John", "a@mail", LocalDate.of(2024,1,15), 1, 1);
        ctx.recordContribution("John", "b@mail", LocalDate.of(2024,1,10), 1, 1);

        AuthorStatistics stats = ctx.getAuthorStatistics().get("John");

        assertThat(stats.getLastCommitDate()).isEqualTo(LocalDate.of(2024,1,10)); // overwritten because method always sets date
    }
}
