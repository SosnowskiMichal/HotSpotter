package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.coupling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.CouplingAnalyzerContext;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CouplingAnalyzerContextTest {

    @Test
    void recordCoupling_ignoresEmptyFileList() {
        CouplingAnalyzerContext ctx = new CouplingAnalyzerContext("A1", null, LocalDate.now());
        ctx.recordCoupling("A", LocalDate.now(), List.of());

        assertTrue(ctx.getFileCommits().isEmpty());
    }

    @Test
    void recordCoupling_ignoresTooLargeCommits() {
        CouplingAnalyzerContext ctx = new CouplingAnalyzerContext("A1", null, LocalDate.now());

        List<String> huge = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            huge.add("File" + i + ".java");
        }

        ctx.recordCoupling("A", LocalDate.now(), huge);
        assertTrue(ctx.getFileCommits().isEmpty());
    }

    @Test
    void recordCoupling_updatesCouplings() {
        CouplingAnalyzerContext ctx = new CouplingAnalyzerContext("A1", null, LocalDate.now());

        ctx.recordCoupling("Alice", LocalDate.now(), List.of("A.java", "B.java", "C.java"));

        assertEquals(1, ctx.getFileCouplings().get("A.java").get("B.java"));
        assertEquals(1, ctx.getFileCouplings().get("B.java").get("C.java"));
        assertEquals(1, ctx.getFileCouplings().get("C.java").get("A.java"));
    }

    @Test
    void updateFilePath_removesOldPath() {
        CouplingAnalyzerContext ctx = new CouplingAnalyzerContext("A1", null, LocalDate.now());

        ctx.getFileCommits().put("old.txt", 5);
        ctx.updateFilePath("old.txt", null);

        assertFalse(ctx.getFileCommits().containsKey("old.txt"));
    }

    @Test
    void updateFilePath_movesCommitCounts() {
        CouplingAnalyzerContext ctx = new CouplingAnalyzerContext("A1", null, LocalDate.now());

        ctx.getFileCommits().put("old.txt", 5);

        ctx.updateFilePath("old.txt", "new.txt");

        assertEquals(5, ctx.getFileCommits().get("new.txt"));
    }
}
