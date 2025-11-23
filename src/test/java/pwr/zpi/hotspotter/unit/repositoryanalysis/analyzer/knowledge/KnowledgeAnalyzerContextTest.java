package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.knowledge;

import org.junit.jupiter.api.Test;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.model.AuthorContribution;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeAnalyzerContextTest {

    @Test
    void recordContribution_shouldCreateAndUpdateContribution() {
        KnowledgeAnalyzerContext ctx =
                new KnowledgeAnalyzerContext("A1", Path.of("/repo"));

        ctx.recordContribution("src/A.java", "John", 10);
        ctx.recordContribution("src/A.java", "John", 5);

        AuthorContribution ac =
                ctx.getFileContributions().get("src/A.java").get("John");

        assertThat(ac).isNotNull();
        assertThat(ac.getLinesAdded()).isEqualTo(15);
        assertThat(ac.getCommits()).isEqualTo(2);
    }

    @Test
    void updateFilePath_shouldMoveEntryToNewPath() {
        KnowledgeAnalyzerContext ctx =
                new KnowledgeAnalyzerContext("A1", Path.of("/repo"));

        ctx.recordContribution("old/X.java", "John", 10);
        ctx.updateFilePath("old/X.java", "new/X.java");

        assertThat(ctx.getFileContributions()).containsKey("new/X.java");
        assertThat(ctx.getFileContributions()).doesNotContainKey("old/X.java");
    }

    @Test
    void updateFilePath_shouldRemoveEntryIfNewPathNull() {
        KnowledgeAnalyzerContext ctx =
                new KnowledgeAnalyzerContext("A1", Path.of("/repo"));

        ctx.recordContribution("old/X.java", "John", 10);
        ctx.updateFilePath("old/X.java", null);

        assertThat(ctx.getFileContributions()).doesNotContainKey("old/X.java");
    }
}
