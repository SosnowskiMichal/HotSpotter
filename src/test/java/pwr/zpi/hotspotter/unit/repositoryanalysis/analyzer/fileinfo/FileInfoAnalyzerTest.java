package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.fileinfo;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FileInfoAnalyzerTest {

    @Mock
    private FileInfoRepository repo;

    @InjectMocks
    private FileInfoAnalyzer analyzer;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void processCommit_shouldHandleRenameAndRecordContribution() {
        Commit commit = mock(Commit.class);
        when(commit.getCommitDateAsLocalDate()).thenReturn(LocalDate.of(2024, 1, 10));

        FileChange f1 = new FileChange("A.java", 0, 0);
        FileChange f2 = new FileChange("NewB.java", 0, 0, "OldB.java", "NewB.java");

        when(commit.changedFiles()).thenReturn(List.of(f1, f2));

        FileInfoAnalyzerContext ctx =
                new FileInfoAnalyzerContext("A1", Path.of("/repo"), LocalDate.of(2024, 1, 20));

        analyzer.processCommit(commit, ctx);

        assertThat(ctx.getFileInfos()).containsKeys("A.java", "NewB.java");
        assertThat(ctx.getFileInfos()).doesNotContainKey("OldB.java");
    }

    @Test
    void finishAnalysis_shouldCalculateSizeAgeLinesAndSave() throws Exception {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        Path repoPath = Path.of("/repo");

        FileInfoAnalyzerContext ctx = new FileInfoAnalyzerContext("A1", repoPath, ref);
        ctx.recordContribution("src/X.java", ref.minusDays(3));

        try (MockedStatic<AnalysisUtils> utils = mockStatic(AnalysisUtils.class);
             MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {

            utils.when(() -> AnalysisUtils.getExistingFileNames(repoPath))
                    .thenReturn(Set.of("src/X.java"));

            utils.when(() -> AnalysisUtils.saveDataInBatches(eq(repo), anyCollection()))
                    .thenAnswer(_ -> null);

            fileUtils.when(() -> FileUtils.sizeOf(any())).thenReturn(100L);
            fileUtils.when(() -> FileUtils.byteCountToDisplaySize(100L)).thenReturn("100 B");

            Process mockProcess = mock(Process.class);
            when(mockProcess.getInputStream())
                    .thenReturn(new java.io.ByteArrayInputStream(
                            ("""
                                    language,file,blank,comment,code
                                    Java,./src/X.java,1,2,3
                                    SUM,0,0,0,0
                                    """).getBytes()
                    ));
            when(mockProcess.waitFor()).thenReturn(0);

            MockedConstruction<ProcessBuilder> processBuilderMock =
                    mockConstruction(ProcessBuilder.class, (pb, _) -> {
                        when(pb.start()).thenReturn(mockProcess);
                        when(pb.directory(any())).thenReturn(pb);
                        when(pb.redirectErrorStream(true)).thenReturn(pb);
                    });

            analyzer.finishAnalysis(ctx);
            processBuilderMock.close();
        }

        FileInfo info = ctx.getFileInfos().get("src/X.java");

        assertThat(info.getFileSize()).isEqualTo("100 B");
        assertThat(info.getCodeAgeDays()).isEqualTo(3);
        assertThat(info.getFileType()).isEqualTo("Java");
        assertThat(info.getCodeLines()).isEqualTo(3);
        assertThat(info.getCommentLines()).isEqualTo(2);
        assertThat(info.getBlankLines()).isEqualTo(1);
        assertThat(info.getTotalLines()).isEqualTo(6);
    }
}
