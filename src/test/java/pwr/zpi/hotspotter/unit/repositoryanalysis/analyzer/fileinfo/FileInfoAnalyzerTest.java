package pwr.zpi.hotspotter.unit.repositoryanalysis.analyzer.fileinfo;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.filter.AnalysisFileFilter;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;
import pwr.zpi.hotspotter.repositoryanalysis.util.RepositoryFileUrlBuilder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileInfoAnalyzerTest {

    @Mock
    private FileInfoRepository repo;
    @Mock
    private AnalysisFileFilter analysisFileFilter;
    @Mock
    private Process clocProcess;

    @InjectMocks
    private FileInfoAnalyzer analyzer;

    @Test
    void processCommit_shouldHandleRenameAndRecordContribution() {
        Commit commit = mock(Commit.class);
        when(commit.getCommitDateAsLocalDate()).thenReturn(LocalDate.of(2024, 1, 10));

        FileChange f1 = new FileChange("A.java", 0, 0);
        FileChange f2 = new FileChange("NewB.java", 0, 0, "OldB.java", "NewB.java");

        when(commit.changedFiles()).thenReturn(List.of(f1, f2));

        FileInfoAnalyzerContext ctx =
                new FileInfoAnalyzerContext("A1", Path.of("/repo"), LocalDate.of(2024, 1, 20), clocProcess);

        analyzer.processCommit(commit, ctx);

        assertThat(ctx.getFileInfos()).containsKeys("A.java", "NewB.java");
        assertThat(ctx.getFileInfos()).doesNotContainKey("OldB.java");
    }

    @Test
    void finishAnalysis_shouldCalculateSizeAgeLinesAndSave() throws Exception {
        LocalDate ref = LocalDate.of(2024, 1, 20);
        Path repoPath = Path.of("/repo");

        FileInfoAnalyzerContext ctx = new FileInfoAnalyzerContext("A1", repoPath, ref, clocProcess);
        ctx.recordContribution("src/X.java", ref.minusDays(3));

        try (MockedStatic<AnalysisUtils> utils = mockStatic(AnalysisUtils.class);
             MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {

            utils.when(() -> AnalysisUtils.getFilteredExistingFileNames(repoPath, analysisFileFilter))
                    .thenReturn(Set.of("src/X.java"));

            utils.when(() -> AnalysisUtils.saveDataInBatches(eq(repo), anyCollection()))
                    .thenAnswer(_ -> null);

            fileUtils.when(() -> FileUtils.sizeOf(any())).thenReturn(100L);
            fileUtils.when(() -> FileUtils.byteCountToDisplaySize(100L)).thenReturn("100 B");

            when(clocProcess.getInputStream())
                    .thenReturn(new java.io.ByteArrayInputStream(
                            ("""
                                    language,file,blank,comment,code
                                    Java,./src/X.java,1,2,3
                                    SUM,0,0,0,0
                            """).getBytes()
                    ));
            when(clocProcess.waitFor()).thenReturn(0);

            MockedConstruction<ProcessBuilder> processBuilderMock =
                    mockConstruction(ProcessBuilder.class, (pb, _) -> {
                        when(pb.start()).thenReturn(clocProcess);
                        when(pb.directory(any())).thenReturn(pb);
                        when(pb.redirectErrorStream(true)).thenReturn(pb);
                    });

            AnalysisInfo analysisInfo = AnalysisInfo.builder()
                    .id("A1")
                    .repositoryPlatform("github")
                    .repositoryUrl("https://github.com/owner/repo")
                    .repositoryOwner("owner")
                    .repositoryName("repo")
                    .lastCommitHash("abc123")
                    .build();

            analyzer.finishAnalysis(ctx, analysisInfo);
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
        assertThat(info.getFileUrl()).isEqualTo("https://github.com/owner/repo/blob/abc123/src/X.java");
    }

}
