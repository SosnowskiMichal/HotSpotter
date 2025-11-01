package pwr.zpi.hotspotter.sonar.service;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarConfig;
import pwr.zpi.hotspotter.sonar.model.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class SonarService {
    private String sonarToken;

    private final SonarConfig sonarConfig;
    private final SonarAnalysisExecutor sonarAnalysisExecutor;
    private final SonarAnalysisStatusRepository sonarAnalysisStatusRepository;
    private final SonarRepoAnalysisRepository sonarRepoAnalysisRepository;

    public SonarService(SonarConfig sonarConfig, SonarAnalysisExecutor sonarAnalysisExecutor, SonarAnalysisStatusRepository sonarAnalysisStatusRepository, SonarRepoAnalysisRepository sonarRepoAnalysisRepository) {
        this.sonarConfig = sonarConfig;
        this.sonarAnalysisExecutor = sonarAnalysisExecutor;
        this.sonarAnalysisStatusRepository = sonarAnalysisStatusRepository;
        this.sonarRepoAnalysisRepository = sonarRepoAnalysisRepository;
    }

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
        if (sonarConfig.validateToken(this.sonarToken)) {
            return true;
        } else {
            sonarConfig.logIn();
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
            SonarAnalysisStatus status = new SonarAnalysisStatus(createValidProjectKey(projectKey), "PENDING", "SonarQube analysis is pending.");
            sonarAnalysisStatusRepository.save(status);

            sonarAnalysisExecutor.runAnalysisAsync(status.getId(), projectPath, status.getProjectKey(), projectName, this.sonarToken);

            return status;
        } else {
            log.error("Failed to prepare SonarQube connection. Analysis not started.");
            return null;
        }
    }

    private boolean setNewSonarToken() {
        String tokenName = "hotspotter-token-" + System.currentTimeMillis();
        this.sonarToken = sonarConfig.generateToken(tokenName);

        return this.sonarToken != null;
    }

    private String createValidProjectKey(String projectPath) {
        return projectPath.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
