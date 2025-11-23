package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.authors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorsAnalyzerTest {

    @Mock
    private AuthorStatisticsRepository authorStatisticsRepository;
    @Mock
    private FileKnowledgeRepository fileKnowledgeRepository;

    @InjectMocks
    private AuthorsAnalyzer analyzer;

    @Test
    void startAnalysis_returnsCorrectContext() {
        LocalDate ref = LocalDate.of(2024,1,10);

        AuthorsAnalyzerContext ctx = analyzer.startAnalysis("A1", ref);

        assertThat(ctx.getAnalysisId()).isEqualTo("A1");
        assertThat(ctx.getReferenceDate()).isEqualTo(ref);
        assertThat(ctx.getAuthorStatistics()).isEmpty();
    }

    @Test
    void processCommit_recordsContributionInContext() {
        AuthorsAnalyzerContext ctx = analyzer.startAnalysis("A1", LocalDate.of(2024,1,15));

        Commit commit = new Commit(
                "c1",
                LocalDate.of(2024,1,10).toString(),
                "John",
                "john@mail.com",
                List.of(
                        new FileChange("A.java", 10, 2),
                        new FileChange("B.java", 3, 1)
                )
        );

        analyzer.processCommit(commit, ctx);

        AuthorStatistics stats = ctx.getAuthorStatistics().get("John");

        assertThat(stats.getCommits()).isEqualTo(1);
        assertThat(stats.getTotalLinesAdded()).isEqualTo(13);
        assertThat(stats.getTotalLinesDeleted()).isEqualTo(3);
        assertThat(stats.getLastCommitDate()).isEqualTo(LocalDate.of(2024,1,10));
        assertThat(stats.getFirstCommitDate()).isEqualTo(LocalDate.of(2024,1,10));
        assertThat(stats.getEmails()).contains("john@mail.com");
    }

    @Test
    void finishAnalysis_calculatesInactivityAndSaves() {
        LocalDate ref = LocalDate.of(2024,1,20);
        AuthorsAnalyzerContext ctx = analyzer.startAnalysis("A1", ref);

        ctx.recordContribution("John", "j@mail", LocalDate.of(2024,1,1), 5, 1);

        analyzer.finishAnalysis(ctx);

        ArgumentCaptor<Iterable<AuthorStatistics>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(authorStatisticsRepository).saveAll(captor.capture());

        List<AuthorStatistics> saved = (List<AuthorStatistics>) captor.getValue();

        AuthorStatistics s = saved.getFirst();
        assertThat(s.getDaysSinceLastCommit()).isEqualTo(19);
        assertThat(s.getMonthsSinceLastCommit()).isEqualTo(0);
        assertThat(s.getIsActive()).isTrue();
    }

    @Test
    void finishAnalysis_handlesExceptionGracefully() {
        LocalDate ref = LocalDate.of(2024,1,20);
        AuthorsAnalyzerContext ctx = analyzer.startAnalysis("A1", ref);

        ctx.recordContribution("John", "j@mail", LocalDate.of(2024,1,1), 5, 1);

        doThrow(new RuntimeException("fail")).when(authorStatisticsRepository).saveAll(any());

        analyzer.finishAnalysis(ctx);

        verify(authorStatisticsRepository).saveAll(any());
    }

    @Test
    void enrichAnalysisData_updatesLeadAndContributorCounts() {
        AuthorsAnalyzerContext ctx = analyzer.startAnalysis("A1", LocalDate.now());
        ctx.recordContribution("Alice", "a@mail", LocalDate.now(), 1, 1);
        ctx.recordContribution("Bob", "b@mail", LocalDate.now(), 1, 1);

        FileKnowledge fk = FileKnowledge.builder()
                .leadAuthor("Alice")
                .authorContributions(List.of(
                        new AuthorContribution("Alice", 10, 2, 50.0),
                        new AuthorContribution("Bob", 5, 1, 25.0)
                ))
                .build();

        when(fileKnowledgeRepository.findAllByAnalysisId("A1")).thenReturn(List.of(fk));

        analyzer.enrichAnalysisData(ctx);

        AuthorStatistics alice = ctx.getAuthorStatistics().get("Alice");
        AuthorStatistics bob = ctx.getAuthorStatistics().get("Bob");

        assertThat(alice.getFilesAsLeadAuthor()).isEqualTo(1);
        assertThat(alice.getExistingFilesModified()).isEqualTo(1);
        assertThat(bob.getExistingFilesModified()).isEqualTo(1);

        verify(authorStatisticsRepository).saveAll(any());
    }

    @Test
    void enrichAnalysisData_handlesExceptionGracefully() {
        AuthorsAnalyzerContext ctx = analyzer.startAnalysis("A1", LocalDate.now());
        ctx.recordContribution("Alice", "a@mail", LocalDate.now(), 1, 1);

        when(fileKnowledgeRepository.findAllByAnalysisId("A1"))
                .thenReturn(List.of());

        doThrow(new RuntimeException("fail")).when(authorStatisticsRepository).saveAll(any());

        analyzer.enrichAnalysisData(ctx);

        verify(authorStatisticsRepository).saveAll(any());
    }
}
