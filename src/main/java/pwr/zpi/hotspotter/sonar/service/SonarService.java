package pwr.zpi.hotspotter.sonar.service;

import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisState;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarService {
    private final SonarClient sonarClient;
    private final SonarProperties sonarProperties;
    private final SonarAnalysisExecutor sonarAnalysisExecutor;
    private final SonarAnalysisStatusRepository sonarAnalysisStatusRepository;
    private final SonarRepoAnalysisRepository sonarRepoAnalysisRepository;


    public SonarAnalysisStatus getSonarAnalysisStatus(String statusId) {
        return sonarAnalysisStatusRepository.findById(statusId).orElseThrow(() ->
                new ObjectNotFoundException("SonarQube analysis status not found for ID: " + statusId));
    }

    public SonarRepoAnalysisResult getSonarRepoAnalysisResult(String analysisId) {
        return sonarRepoAnalysisRepository.findById(analysisId).orElseThrow(() ->
                new ObjectNotFoundException("SonarQube analysis result not found for ID: " + analysisId));
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

    public SonarAnalysisStatus startAnalysis(String projectPath, String projectKey, String projectName) {
        Path path = Path.of(projectPath);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            log.error("Project path does not exist or is not a directory: {}", projectPath);
            throw new ObjectNotFoundException("Project path does not exist or is not a directory");
        }

        if (prepareConnection()) {
            SonarAnalysisStatus status = new SonarAnalysisStatus(createValidProjectKey(projectKey), SonarAnalysisState.PENDING, "SonarQube analysis is pending.");
            sonarAnalysisStatusRepository.save(status);

            sonarAnalysisExecutor.runAnalysisAsync(status.getId(), projectPath, status.getProjectKey(), projectName);

            return status;
        } else {
            log.error("Failed to prepare SonarQube connection. Analysis not started.");
            return null;
        }
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
