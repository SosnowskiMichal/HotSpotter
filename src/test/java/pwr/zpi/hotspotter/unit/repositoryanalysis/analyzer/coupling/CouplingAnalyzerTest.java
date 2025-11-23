package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.coupling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.CouplingAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.CouplingAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.AuthorCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.repository.FileCouplingRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouplingAnalyzerTest {

    @Mock
    private FileCouplingRepository fileCouplingRepository;
    @Mock
    private AuthorCouplingRepository authorCouplingRepository;

    @InjectMocks
    private CouplingAnalyzer analyzer;

    private final Path repoPath = Path.of("repo");
    private final LocalDate referenceDate = LocalDate.of(2024, 1, 15);


    @Test
    void startAnalysis_createsContext() {
        CouplingAnalyzerContext ctx = analyzer.startAnalysis("A1", repoPath, referenceDate);

        assertEquals("A1", ctx.getAnalysisId());
        assertEquals(repoPath, ctx.getRepositoryPath());
        assertEquals(referenceDate, ctx.getReferenceDate());
        assertEquals(referenceDate.minusMonths(12), ctx.getAuthorCouplingAnalysisStartDate());
    }

    @Test
    void processCommit_recordsCouplingCorrectly() {
        CouplingAnalyzerContext ctx = analyzer.startAnalysis("A1", repoPath, referenceDate);

        Commit commit = mock(Commit.class);
        when(commit.author()).thenReturn("Alice");
        when(commit.getCommitDateAsLocalDate()).thenReturn(LocalDate.of(2024, 1, 10));
        when(commit.changedFiles()).thenReturn(List.of(
                new FileChange("A.java", 10, 0),
                new FileChange("B.java", 5, 3)
        ));

        analyzer.processCommit(commit, ctx);

        assertEquals(1, ctx.getFileCommits().get("A.java"));
        assertEquals(1, ctx.getFileCommits().get("B.java"));

        assertEquals(1, ctx.getFileCouplings().get("A.java").get("B.java"));
        assertEquals(1, ctx.getFileCouplings().get("B.java").get("A.java"));

        assertEquals(1, ctx.getAuthorFileChanges().get("Alice").get("A.java"));
    }

    @Test
    void processCommit_handlesRenamedPaths() {
        CouplingAnalyzerContext ctx = analyzer.startAnalysis("A1", repoPath, referenceDate);

        Commit commit = mock(Commit.class);
        when(commit.author()).thenReturn("Bob");
        when(commit.getCommitDateAsLocalDate()).thenReturn(referenceDate);
        when(commit.changedFiles()).thenReturn(List.of(
                new FileChange("new.txt", 1, 0, "old.txt", "new.txt")
        ));

        analyzer.processCommit(commit, ctx);

        assertNull(ctx.getFileCommits().get("old.txt"));
        assertEquals(1, ctx.getFileCommits().get("new.txt"));
    }

    @Test
    void finishAnalysis_savesFileAndAuthorCouplings() {
        CouplingAnalyzerContext ctx = analyzer.startAnalysis("A1", repoPath, referenceDate);

        ctx.getFileCommits().put("A.java", 10);
        ctx.getFileCouplings().put("A.java",
                Map.of("B.java", 6)
        );

        ctx.getAuthorFileChanges().put("Alice",
                Map.of("A.java", 6, "B.java", 4, "C.java", 3)
        );
        ctx.getAuthorFileChanges().put("Bob",
                Map.of("A.java", 5, "B.java", 3, "C.java", 3)
        );

        try (MockedStatic<AnalysisUtils> utils = Mockito.mockStatic(AnalysisUtils.class)) {
            utils.when(() -> AnalysisUtils.getExistingFileNames(repoPath))
                    .thenReturn(Set.of("A.java", "B.java", "C.java"));

            analyzer.finishAnalysis(ctx);

            utils.verify(() -> AnalysisUtils.saveDataInBatches(eq(fileCouplingRepository), anyList()));
            utils.verify(() -> AnalysisUtils.saveDataInBatches(eq(authorCouplingRepository), anyList()));
        }
    }
}
