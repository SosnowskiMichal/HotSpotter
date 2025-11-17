package pwr.zpi.hotspotter.unit.sonar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.util.Pair;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarFileAnalysisResult;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarFileAnalysisRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;
import pwr.zpi.hotspotter.sonar.service.JavaProjectCompiler;
import pwr.zpi.hotspotter.sonar.service.SonarAnalysisExecutor;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SonarAnalysisExecutorTest {

    @Mock
    private SonarAnalysisStatusRepository sonarAnalysisStatusRepository;
    @Mock
    private JavaProjectCompiler javaProjectCompiler;
    @Mock
    private SonarProperties sonarProperties;
    @Mock
    private SonarRepoAnalysisRepository sonarRepoAnalysisRepository;
    @Mock
    private SonarFileAnalysisRepository sonarFileAnalysisRepository;

    @InjectMocks
    private SonarAnalysisExecutor sonarAnalysisExecutor;

    @Test
    void runAnalysisAsyncUpdatesStatusToFailedWhenSonarScannerFails() throws Exception {
        String repoAnalysisId = "repo-id";
        String sonarAnalysisId = "sonar-id";
        Path projectPath = Path.of("/project/path");
        String projectKey = "project-key";
        String projectName = "project-name";

        SonarAnalysisStatus status = new SonarAnalysisStatus();
        status.setProjectKey(projectKey);
        when(sonarAnalysisStatusRepository.findById(sonarAnalysisId)).thenReturn(Optional.of(status));
        when(sonarProperties.getScannerPath()).thenReturn("/path/to/scanner");
        when(sonarProperties.getHostUrl()).thenReturn("https://sonar.example.com");
        when(sonarProperties.getToken()).thenReturn("token");
        when(javaProjectCompiler.findCommonJavaSourceRoot(projectPath)).thenReturn(Optional.empty());

        CompletableFuture<Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult>> result =
                sonarAnalysisExecutor.runAnalysisAsync(repoAnalysisId, sonarAnalysisId, projectPath, projectKey, projectName);

        assertNotNull(result);
        verify(sonarAnalysisStatusRepository, times(2)).save(any(SonarAnalysisStatus.class));
        verify(sonarRepoAnalysisRepository, never()).save(any(SonarRepoAnalysisResult.class));
        verify(sonarFileAnalysisRepository, never()).save(any(SonarFileAnalysisResult.class));
    }

    @Test
    void runAnalysisAsyncThrowsObjectNotFoundExceptionWhenStatusNotFound() {
        String repoAnalysisId = "repo-id";
        String sonarAnalysisId = "sonar-id";
        Path projectPath = Path.of("/project/path");
        String projectKey = "project-key";
        String projectName = "project-name";

        when(sonarAnalysisStatusRepository.findById(sonarAnalysisId)).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () ->
                sonarAnalysisExecutor.runAnalysisAsync(repoAnalysisId, sonarAnalysisId, projectPath, projectKey, projectName));
    }
}
