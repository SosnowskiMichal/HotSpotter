package pwr.zpi.hotspotter.unit.sonar.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisState;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarIssue;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisComponent;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.*;
import pwr.zpi.hotspotter.sonar.service.JavaProjectCompiler;
import pwr.zpi.hotspotter.sonar.service.SonarAnalysisExecutor;
import pwr.zpi.hotspotter.sonar.service.SonarResultDownloader;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SonarAnalysisExecutorTest {

    @Mock private SonarAnalysisStatusRepository statusRepo;
    @Mock private SonarResultDownloader resultDownloader;
    @Mock private JavaProjectCompiler compiler;
    @Mock private SonarProperties sonarProperties;
    @Mock private SonarRepoAnalysisRepository repoAnalysisRepo;
    @Mock private SonarIssueRepository sonarIssueRepository;
    @Mock private SonarRepoAnalysisComponentRepository componentRepo;

    @InjectMocks
    @Spy
    private SonarAnalysisExecutor executor;

    private final Path projectPath = Path.of("/tmp/project");

    private SonarAnalysisStatus status() {
        SonarAnalysisStatus s = new SonarAnalysisStatus();
        s.setId("A1");
        s.setProjectKey("project-key");
        return s;
    }

    private SonarResultDownloader.SonarAnalysisResults mockResults() {
        return new SonarResultDownloader.SonarAnalysisResults(
                new SonarRepoAnalysisResult(),
                List.of(new SonarRepoAnalysisComponent()),
                List.of(new SonarIssue())
        );
    }

    @Test
    void runAnalysisAsync_Throws_WhenStatusNotFound() {
        when(statusRepo.findById("A1")).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () ->
                executor.runAnalysisAsync("R1", "A1", projectPath, "key", "name")
        );
    }

    @Test
    void runAnalysisAsync_ReturnsResults_WhenScannerAndDownloadSucceed() {
        SonarAnalysisStatus s = status();
        when(statusRepo.findById("A1")).thenReturn(Optional.of(s));
        when(executor.executeSonarScanner(any(), any(), any())).thenReturn(true);

        SonarResultDownloader.SonarAnalysisResults results = mockResults();
        doReturn(results).when(executor).getAndSaveResults(anyString(), anyString());

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> future =
                executor.runAnalysisAsync("R1", "A1", projectPath, "key", "name");

        assertNotNull(future);
        assertEquals(results, future.join());
        verify(statusRepo, atLeastOnce()).save(any());
        assertEquals(SonarAnalysisState.SUCCESS, s.getStatus());
    }

    @Test
    void runAnalysisAsync_SetsFailed_WhenScannerFails() {
        SonarAnalysisStatus s = status();
        when(statusRepo.findById("A1")).thenReturn(Optional.of(s));
        when(executor.executeSonarScanner(any(), any(), any())).thenReturn(false);

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> future =
                executor.runAnalysisAsync("R1", "A1", projectPath, "key", "name");

        assertNull(future.join());
        verify(statusRepo, atLeastOnce()).save(any());
        assertEquals(SonarAnalysisState.FAILED, s.getStatus());
    }

    @Test
    void runAnalysisAsync_SetsFailed_WhenExceptionOccurs() {
        SonarAnalysisStatus s = status();
        when(statusRepo.findById("A1")).thenReturn(Optional.of(s));

        doThrow(new RuntimeException("boom"))
                .when(executor).executeSonarScanner(any(), any(), any());

        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> future =
                executor.runAnalysisAsync("R1", "A1", projectPath, "key", "name");

        assertNull(future.join());
        assertEquals(SonarAnalysisState.FAILED, s.getStatus());
    }

    @Test
    void getAndSaveResults_SavesAndReturnsResults_WhenPresent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        SonarResultDownloader.SonarAnalysisResults results = mockResults();
        when(resultDownloader.fetchAnalysisResults("R1", "project-key")).thenReturn(results);

        SonarResultDownloader.SonarAnalysisResults _ =
                (SonarResultDownloader.SonarAnalysisResults) executor.getClass()
                        .getDeclaredMethod("getAndSaveResults", String.class, String.class)
                        .invoke(executor, "R1", "project-key");

        verify(repoAnalysisRepo).save(any());
        verify(componentRepo).saveAll(any());
        verify(sonarIssueRepository).saveAll(any());
    }
}