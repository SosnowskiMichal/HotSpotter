package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.fileinfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.common.cloc.model.FileLinesData;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FileInfoAnalyzerContextTest {

    @Mock
    CompletableFuture<Map<String, FileLinesData>> clocFuture;

    @Test
    void recordContribution_shouldCreateNewEntry() {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        FileInfoAnalyzerContext ctx = new FileInfoAnalyzerContext("A1", Path.of("/repo"), ref, clocFuture);

        ctx.recordContribution("src/Main.java", ref.minusDays(3));

        FileInfo info = ctx.getFileInfos().get("src/Main.java");

        assertThat(info).isNotNull();
        assertThat(info.getFilePath()).isEqualTo("src/Main.java");
        assertThat(info.getFileName()).isEqualTo("Main.java");
        assertThat(info.getFirstCommitDate()).isEqualTo(ref.minusDays(3));
        assertThat(info.getTotalCommits()).isEqualTo(1);
    }

    @Test
    void recordContribution_shouldUpdateCountersCorrectly() {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        FileInfoAnalyzerContext ctx = new FileInfoAnalyzerContext("A1", Path.of("/repo"), ref, clocFuture);

        ctx.recordContribution("file.txt", ref.minusDays(10));
        ctx.recordContribution("file.txt", ref.minusMonths(3));

        FileInfo info = ctx.getFileInfos().get("file.txt");

        assertThat(info.getCommitsInHotspotAnalysisPeriod()).isEqualTo(2);
        assertThat(info.getCommitsLastMonth()).isEqualTo(1);
        assertThat(info.getCommitsLastYear()).isEqualTo(2);
        assertThat(info.getTotalCommits()).isEqualTo(2);
        assertThat(info.getLastCommitDate()).isEqualTo(ref.minusMonths(3));
    }

    @Test
    void updateFilePath_shouldMoveEntryToNewKey() {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        FileInfoAnalyzerContext ctx = new FileInfoAnalyzerContext("A1", Path.of("/repo"), ref, clocFuture);

        ctx.recordContribution("old/File.java", ref.minusDays(1));

        ctx.updateFilePath("old/File.java", "new/File.java");

        assertThat(ctx.getFileInfos()).containsKey("new/File.java");
        assertThat(ctx.getFileInfos()).doesNotContainKey("old/File.java");

        FileInfo info = ctx.getFileInfos().get("new/File.java");
        assertThat(info.getFileName()).isEqualTo("File.java");
    }

    @Test
    void updateFilePath_shouldRemoveWhenNewPathNull() {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        FileInfoAnalyzerContext ctx = new FileInfoAnalyzerContext("A1", Path.of("/repo"), ref, clocFuture);

        ctx.recordContribution("file.txt", ref.minusDays(1));
        ctx.updateFilePath("file.txt", null);

        assertThat(ctx.getFileInfos()).doesNotContainKey("file.txt");
    }

}