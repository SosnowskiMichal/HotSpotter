package pwr.zpi.hotspotter.sonar.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pwr.zpi.hotspotter.sonar.model.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;

@Slf4j
@Service
public class SonarAnalysisExecutor {

    @Value("${sonar.host.url}")
    private String sonarUrl;

    @Value("${sonarqube.scanner.path:sonar-scanner}")
    private String scannerPath;

    private final RestTemplate restTemplate;

    private final SonarAnalysisStatusRepository sonarAnalysisStatusRepository;

    public SonarAnalysisExecutor(RestTemplateBuilder restTemplateBuilder, SonarAnalysisStatusRepository sonarAnalysisStatusRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.sonarAnalysisStatusRepository = sonarAnalysisStatusRepository;
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
                status.setStatus("SUCCESS");
                status.setMessage("Analiza zakończona pomyślnie");

                String taskId = getLatestAnalysisTask(projectKey, token);
                status.setTaskId(taskId);

                saveResults(status.getId());
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

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[SonarScanner] {}", line);
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            log.error("Błąd podczas wykonywania sonar-scanner: {}", e.getMessage());
            return false;
        }
    }

    private String getLatestAnalysisTask(String projectKey, String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = sonarUrl + "/api/ce/component?component=" + projectKey;

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("queue")) {
                return null;
            }

            if (body != null && body.containsKey("current")) {
                Map<String, Object> current = (Map<String, Object>) body.get("current");
                return (String) current.get("id");
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveResults(String analysisId) {
        // Implementacja zapisywania wyników analizy
    }
}