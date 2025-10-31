package pwr.zpi.hotspotter.sonar.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.sonar.model.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;

import java.io.File;

@Slf4j
@Service
public class SonarAnalysisExecutor {

    @Value("${sonar.host.url}")
    private String sonarUrl;

    @Value("${sonarqube.scanner.path:sonar-scanner}")
    private String scannerPath;

    private final static int MILLISECONDS_TO_WAIT_BEFORE_FETCHING_RESULTS = 10000;

    private final SonarAnalysisStatusRepository sonarAnalysisStatusRepository;
    private final SonarResultDownloader sonarResultDownloader;

    public SonarAnalysisExecutor(SonarAnalysisStatusRepository sonarAnalysisStatusRepository, SonarResultDownloader sonarResultDownloader) {
        this.sonarAnalysisStatusRepository = sonarAnalysisStatusRepository;
        this.sonarResultDownloader = sonarResultDownloader;
    }


    @Async("sonarQubeAnalysisExecutor")
    public void runAnalysisAsync(String analysisId, String projectPath, String projectKey, String projectName, String token) {
        SonarAnalysisStatus status = sonarAnalysisStatusRepository.findById(analysisId).orElseThrow(() ->
                new RuntimeException("Nie znaleziono statusu analizy o ID: " + analysisId));

        try {
            status.setStatus("RUNNING");
            status.setMessage("Analiza w toku...");
            sonarAnalysisStatusRepository.save(status);

            boolean success = executeSonarScanner(projectPath, projectKey, projectName, token);

            if (success) {
                Thread.sleep(MILLISECONDS_TO_WAIT_BEFORE_FETCHING_RESULTS);
                SonarRepoAnalysisResult saveResult = saveResults(status.getProjectKey(), token);
                if (saveResult == null) {
                    throw new RuntimeException("Nie udało się pobrać lub zapisać wyników analizy SonarQube.");
                }

                status.setStatus("SUCCESS");
                status.setMessage("Analiza zakończona pomyślnie");
            } else {
                status.setStatus("FAILED");
                status.setMessage("Analiza zakończona z błędem");
            }

        } catch (Exception e) {
            status.setStatus("FAILED");
            status.setMessage("Błąd podczas analizy: " + e.getMessage());
        } finally {
            status.setEndTime(System.currentTimeMillis());
            sonarAnalysisStatusRepository.save(status);
        }
    }


    private boolean executeSonarScanner(String projectPath, String projectKey, String projectName, String token) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    scannerPath,
                    "-Dsonar.projectKey=" + projectKey,
                    "-Dsonar.projectName=" + projectName,
                    "-Dsonar.sources=.",
                    "-Dsonar.host.url=" + sonarUrl,
                    "-Dsonar.token=" + token
            );

            processBuilder.directory(new File(projectPath));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            log.error("Błąd podczas wykonywania sonar-scanner: {}", e.getMessage());
            return false;
        }
    }

    private SonarRepoAnalysisResult saveResults(String projectKey, String token) {
        return sonarResultDownloader.fetchAndSaveAnalysisResults(projectKey, token);
    }
}