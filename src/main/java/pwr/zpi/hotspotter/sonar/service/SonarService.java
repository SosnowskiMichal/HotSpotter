package pwr.zpi.hotspotter.sonar.service;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.sonar.config.SonarConfig;
import pwr.zpi.hotspotter.sonar.model.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;

@Slf4j
@Service
public class SonarService {
    private String sonarToken;

    private final SonarConfig sonarConfig;
    private final SonarAnalysisExecutor sonarAnalysisExecutor;
    private final SonarAnalysisStatusRepository sonarAnalysisStatusRepository;

    public SonarService(SonarConfig sonarConfig, SonarAnalysisExecutor sonarAnalysisExecutor, SonarAnalysisStatusRepository sonarAnalysisStatusRepository) {
        this.sonarConfig = sonarConfig;
        this.sonarAnalysisExecutor = sonarAnalysisExecutor;
        this.sonarAnalysisStatusRepository = sonarAnalysisStatusRepository;
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

    public String startAnalysis(String projectPath, String projectKey, String projectName) {
        if (prepareConnection()) {
            SonarAnalysisStatus status = new SonarAnalysisStatus(projectKey, "PENDING", "Analiza oczekuje na rozpoczęcie");
            sonarAnalysisStatusRepository.save(status);
            String analysisId = status.getId();

            sonarAnalysisExecutor.runAnalysisAsync(analysisId, projectPath, projectKey, projectName, this.sonarToken);

            return analysisId;
        } else {
            log.error("Nie udało się przygotować połączenia z SonarQube. Analiza nie została uruchomiona.");
            return null;
        }
    }

    private boolean setNewSonarToken() {
        String tokenName = "hotspotter-token-" + System.currentTimeMillis();
        this.sonarToken = sonarConfig.generateToken(tokenName);

        return this.sonarToken != null;
    }
}
