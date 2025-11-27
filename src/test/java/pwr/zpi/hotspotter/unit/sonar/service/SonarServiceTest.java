package pwr.zpi.hotspotter.unit.sonar.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pwr.zpi.hotspotter.common.exception.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisState;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;
import pwr.zpi.hotspotter.sonar.service.SonarAnalysisExecutor;
import pwr.zpi.hotspotter.sonar.service.SonarClient;
import pwr.zpi.hotspotter.sonar.service.SonarResultDownloader;
import pwr.zpi.hotspotter.sonar.service.SonarService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SonarServiceTest {

    @Mock
    private SonarClient sonarClient;
    @Mock
    private SonarProperties sonarProperties;
    @Mock
    private SonarAnalysisExecutor sonarAnalysisExecutor;
    @Mock
    private SonarAnalysisStatusRepository sonarAnalysisStatusRepository;

    @InjectMocks
    private SonarService sonarService;

    @Test
    void getSonarAnalysisStatusShouldReturnStatusWhenExists() {
        String repoAnalysisId = "123";
        SonarAnalysisStatus expectedStatus = new SonarAnalysisStatus();
        when(sonarAnalysisStatusRepository.findByRepoAnalysisId(repoAnalysisId))
                .thenReturn(Optional.of(expectedStatus));

        SonarAnalysisStatus result = sonarService.getSonarAnalysisStatus(repoAnalysisId);

        assertEquals(expectedStatus, result);
    }

    @Test
    void getSonarAnalysisStatusShouldThrowExceptionWhenNotFound() {
        String repoAnalysisId = "123";
        when(sonarAnalysisStatusRepository.findByRepoAnalysisId(repoAnalysisId))
                .thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () -> sonarService.getSonarAnalysisStatus(repoAnalysisId));
    }

    @Test
    void prepareConnectionShouldReturnTrueWhenTokenIsValid() {
        when(sonarClient.validateToken(anyString())).thenReturn(true);
        when(sonarProperties.getToken()).thenReturn("token");

        boolean result = sonarService.prepareConnection();

        assertTrue(result);
        verify(sonarClient, never()).logIn();
    }

    @Test
    void prepareConnectionShouldLogInAndSetNewTokenWhenTokenIsInvalid() {
        when(sonarClient.validateToken(null)).thenReturn(false);
        when(sonarClient.generateToken(anyString())).thenReturn("new-token");

        AtomicReference<String> tokenRef = new AtomicReference<>(null);
        when(sonarProperties.getToken()).thenAnswer(_ -> tokenRef.get());
        doAnswer(invocation -> {
            tokenRef.set(invocation.getArgument(0));
            return null;
        }).when(sonarProperties).setToken(anyString());


        boolean result = sonarService.prepareConnection();

        assertTrue(result);
        verify(sonarClient).logIn();
        verify(sonarProperties).setToken("new-token");
    }

    @Test
    void runAnalysisShouldThrowExceptionWhenProjectPathDoesNotExist() {
        Path projectPath = Path.of("/invalid/path");
        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(projectPath)).thenReturn(false);

            assertThrows(ObjectNotFoundException.class, () -> sonarService.runAnalysis("123", projectPath, "key", "name"));
        }
    }

    @Test
    void runAnalysisShouldThrowExceptionWhenAnalysisAlreadyStarted() {
        String projectKey = "key";
        String repoAnalysisId = "123";
        when(sonarAnalysisStatusRepository.findFirstByProjectKeyOrderByStartTimeDesc(projectKey))
                .thenReturn(Optional.of(new SonarAnalysisStatus(repoAnalysisId, projectKey, SonarAnalysisState.PENDING, "")));

        assertThrows(IllegalStateException.class, () -> sonarService.runAnalysis("123", Path.of("/valid/path"), projectKey, "name"));
    }

    @Test
    void runAnalysisShouldStartAnalysisWhenConditionsAreMet() {
        String repoAnalysisId = "123";
        Path projectPath = Path.of("/valid/path");
        String projectKey = "key";
        String projectName = "name";
        SonarAnalysisStatus status = new SonarAnalysisStatus(repoAnalysisId, projectKey, SonarAnalysisState.PENDING, "Pending");
        status.setId(repoAnalysisId);
        when(sonarProperties.getToken()).thenReturn("token");
        when(sonarClient.validateToken(anyString())).thenReturn(true);
        when(sonarAnalysisStatusRepository.save(any(SonarAnalysisStatus.class))).thenReturn(status);
        CompletableFuture<SonarResultDownloader.SonarAnalysisResults> future = new CompletableFuture<>();
        when(sonarAnalysisExecutor.runAnalysisAsync(anyString(), anyString(), any(), anyString(), anyString())).thenReturn(future);

        try (var filesMock = mockStatic(Files.class)) {
            filesMock.when(() -> Files.exists(projectPath)).thenReturn(true);
            filesMock.when(() -> Files.isDirectory(projectPath)).thenReturn(true);

            CompletableFuture<SonarResultDownloader.SonarAnalysisResults> result = sonarService.runAnalysis(repoAnalysisId, projectPath, projectKey, projectName);

            assertNotNull(result);
            verify(sonarAnalysisStatusRepository).save(any(SonarAnalysisStatus.class));
            verify(sonarAnalysisExecutor).runAnalysisAsync(anyString(), anyString(), any(), anyString(), anyString());
        }
    }
}
