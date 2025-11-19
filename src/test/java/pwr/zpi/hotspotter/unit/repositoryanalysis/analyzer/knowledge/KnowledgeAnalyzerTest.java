package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.knowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.FileKnowledge;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.KnowledgeRisk;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.repository.FileKnowledgeRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

import java.nio.file.Path;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KnowledgeAnalyzerTest {

    @Mock
    private FileKnowledgeRepository fileKnowledgeRepository;

    @Mock
    private AuthorStatisticsRepository authorStatisticsRepository;

    @InjectMocks
    private KnowledgeAnalyzer analyzer;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processCommit_shouldRecordContributionsAndHandleRename() {
        KnowledgeAnalyzerContext ctx =
                new KnowledgeAnalyzerContext("A1", Path.of("/repo"));

        Commit commit = mock(Commit.class);
        when(commit.author()).thenReturn("Alice");

        FileChange fc1 = new FileChange("A.java", 10, 0);
        FileChange fc2 = new FileChange("NewB.java", 5, 0, "OldB.java", "NewB.java");

        when(commit.changedFiles()).thenReturn(List.of(fc1, fc2));

        analyzer.processCommit(commit, ctx);

        assertThat(ctx.getFileContributions()).containsKeys("A.java", "NewB.java");
        assertThat(ctx.getFileContributions()).doesNotContainKey("OldB.java");

        AuthorContribution ac = ctx.getFileContributions().get("A.java").get("Alice");
        assertThat(ac.getLinesAdded()).isEqualTo(10);
    }

    @Test
    void finishAnalysis_shouldFilterFilesAndSaveToRepository() {
        KnowledgeAnalyzerContext ctx =
                new KnowledgeAnalyzerContext("A1", Path.of("/repo"));

        ctx.recordContribution("A.java", "John", 10);
        ctx.recordContribution("B.java", "Kate", 20);

        try (MockedStatic<AnalysisUtils> utils = mockStatic(AnalysisUtils.class)) {

            utils.when(() -> AnalysisUtils.getExistingFileNames(Path.of("/repo")))
                    .thenReturn(Set.of("A.java"));

            utils.when(() ->
                            AnalysisUtils.saveDataInBatches(eq(fileKnowledgeRepository), anyCollection()))
                    .thenAnswer(_ -> null);

            analyzer.finishAnalysis(ctx);

            ArgumentCaptor<Collection<FileKnowledge>> captor =
                    ArgumentCaptor.forClass(Collection.class);

            utils.verify(
                    () -> AnalysisUtils.saveDataInBatches(eq(fileKnowledgeRepository), captor.capture())
            );

            Collection<FileKnowledge> saved = captor.getValue();

            assertThat(saved).hasSize(1);
            FileKnowledge fk = saved.iterator().next();
            assertThat(fk.getFilePath()).isEqualTo("A.java");
            assertThat(fk.getLeadAuthor()).isEqualTo("John");
        }
    }

    @Test
    void enrichAnalysisData_shouldComputeKnowledgeLossAndRisk() {
        KnowledgeAnalyzerContext ctx =
                new KnowledgeAnalyzerContext("A1", Path.of("/repo"));

        AuthorContribution c1 = new AuthorContribution("John");
        c1.increaseLinesAdded(80);
        c1.incrementCommits();

        AuthorContribution c2 = new AuthorContribution("Kate");
        c2.increaseLinesAdded(20);
        c2.incrementCommits();

        FileKnowledge fk = FileKnowledge.builder()
                .analysisId("A1")
                .filePath("A.java")
                .linesAdded(100)
                .commits(2)
                .authorContributions(List.of(c1, c2))
                .build();

        when(fileKnowledgeRepository.findAllByAnalysisId("A1"))
                .thenReturn(List.of(fk));

        AuthorStatistics as1 = AuthorStatistics.builder()
                .analysisId("A1")
                .name("John")
                .isActive(false)
                .build();

        AuthorStatistics as2 = AuthorStatistics.builder()
                .analysisId("A1")
                .name("Kate")
                .isActive(true)
                .build();

        when(authorStatisticsRepository.findAllByAnalysisId("A1"))
                .thenReturn(List.of(as1, as2));

        try (MockedStatic<AnalysisUtils> utils = mockStatic(AnalysisUtils.class)) {

            utils.when(() ->
                            AnalysisUtils.saveDataInBatches(eq(fileKnowledgeRepository), anyCollection()))
                    .thenAnswer(_ -> null);

            analyzer.enrichAnalysisData(ctx);

            assertThat(fk.getActiveAuthors()).isEqualTo(1);

            assertThat(fk.getKnowledgeLoss()).isEqualTo(80.0);
            assertThat(fk.getKnowledgeRisk()).isEqualTo(KnowledgeRisk.ABANDONED);
        }
    }
}
