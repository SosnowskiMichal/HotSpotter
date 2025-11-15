package pwr.zpi.hotspotter.sonar.service;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisState;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarFileAnalysisResult;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarService {
    private static final int PENDING_ANALYSIS_TIMEOUT_MS = 60 * 1000;
    private static final int RUNNING_ANALYSIS_TIMEOUT_MS = PENDING_ANALYSIS_TIMEOUT_MS * 15;

    private final SonarClient sonarClient;
    private final SonarProperties sonarProperties;
    private final SonarAnalysisExecutor sonarAnalysisExecutor;
    private final SonarAnalysisStatusRepository sonarAnalysisStatusRepository;
    private final SonarRepoAnalysisRepository sonarRepoAnalysisRepository;


    public SonarAnalysisStatus getSonarAnalysisStatus(String repoAnalysisId) {
        return sonarAnalysisStatusRepository.findByRepoAnalysisId(repoAnalysisId).orElseThrow(() ->
                new ObjectNotFoundException("SonarQube analysis status not found for ID: " + repoAnalysisId));
    }

    public SonarRepoAnalysisResult getSonarRepoAnalysisResult(String repoAnalysisId) {
        return sonarRepoAnalysisRepository.findByRepoAnalysisId(repoAnalysisId).orElseThrow(() ->
                new ObjectNotFoundException("SonarQube analysis result not found for ID: " + repoAnalysisId));
    }

    @Synchronized
    public boolean prepareConnection() {
        if (sonarClient.validateToken(sonarProperties.getToken())) {
            return true;
        } else {
            sonarClient.logIn();
            return setNewSonarToken();
        }
    }

    public CompletableFuture<Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult>> runAnalysis(String repoAnalysisId, Path projectPath, String projectKey, String projectName) {
        projectKey = createValidProjectKey(projectKey);
        if (isAnalysisStarted(projectKey)) {
            log.info("Analysis already started for project key: {}", projectKey);
            throw new IllegalStateException("Analysis already started for this project.");
        }

        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.error("Project path does not exist or is not a directory: {}", projectPath);
            throw new ObjectNotFoundException("Project path does not exist or is not a directory");
        }

        if (prepareConnection()) {
            SonarAnalysisStatus status = new SonarAnalysisStatus(repoAnalysisId, projectKey, SonarAnalysisState.PENDING, "SonarQube analysis is pending.");
            sonarAnalysisStatusRepository.save(status);

            return sonarAnalysisExecutor.runAnalysisAsync(repoAnalysisId, status.getId(), projectPath, status.getProjectKey(), projectName);
        } else {
            log.error("Failed to prepare SonarQube connection. Analysis not started.");
            return null;
        }
    }

    private boolean isAnalysisStarted(String projectKey) {
        SonarAnalysisStatus status = sonarAnalysisStatusRepository
                .findFirstByProjectKeyOrderByStartTimeDesc(projectKey)
                .orElse(null);
        if (status == null) {
            return false;
        }

        LocalDateTime start = status.getStartTime();
        if (start == null) {
            return false;
        }

        long elapsedMs = Duration.between(start, LocalDateTime.now()).toMillis();

        return (status.getStatus() == SonarAnalysisState.PENDING && elapsedMs < PENDING_ANALYSIS_TIMEOUT_MS)
                || (status.getStatus() == SonarAnalysisState.RUNNING && elapsedMs < RUNNING_ANALYSIS_TIMEOUT_MS);
    }

    private boolean setNewSonarToken() {
        String tokenName = "hotspotter-token-" + System.currentTimeMillis();
        sonarProperties.setToken(sonarClient.generateToken(tokenName));

        return sonarProperties.getToken() != null;
    }

    private String createValidProjectKey(String projectPath) {
        return projectPath.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
