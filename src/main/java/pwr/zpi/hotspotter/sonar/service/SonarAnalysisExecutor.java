package pwr.zpi.hotspotter.sonar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisState;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarAnalysisExecutor {
    private final static int MILLISECONDS_TO_WAIT_BEFORE_FETCHING_RESULTS = 10000;
    private final static int MAX_DOWNLOAD_ATTEMPTS = 15;
    private final static int DELAY_BETWEEN_DOWNLOAD_ATTEMPTS_MS = 10000;

    private final SonarAnalysisStatusRepository sonarAnalysisStatusRepository;
    private final SonarResultDownloader sonarResultDownloader;
    private final JavaProjectCompiler javaProjectCompiler;
    private final SonarProperties sonarProperties;
    private final SonarRepoAnalysisRepository sonarRepoAnalysisRepository;


    @Async("repoAnalysisExecutor")
    public CompletableFuture<SonarRepoAnalysisResult> runAnalysisAsync(String repoAnalysisId, String sonarAnalysisId, Path projectPath, String projectKey, String projectName) {
        SonarAnalysisStatus status = sonarAnalysisStatusRepository.findById(sonarAnalysisId).orElseThrow(() ->
                new ObjectNotFoundException("SonarQube analysis status not found for ID: " + sonarAnalysisId));

        SonarRepoAnalysisResult sonarRepoAnalysisResult = null;

        try {
            status.setStatus(SonarAnalysisState.RUNNING);
            status.setMessage("SonarQube analysis is running.");
            log.info("SonarQube analysis is running.");
            sonarAnalysisStatusRepository.save(status);

            boolean success = executeSonarScanner(projectPath, projectKey, projectName);

            if (success) {
                Thread.sleep(MILLISECONDS_TO_WAIT_BEFORE_FETCHING_RESULTS);
                sonarRepoAnalysisResult = getAndSaveResults(repoAnalysisId, status.getProjectKey());
                if (sonarRepoAnalysisResult == null) {
                    throw new RuntimeException("Failed to fetch/save analysis results for project: " + status.getProjectKey());
                }

                sonarRepoAnalysisResult.setRepoAnalysisId(repoAnalysisId);
                status.setStatus(SonarAnalysisState.SUCCESS);
                status.setMessage("SonarQube analysis completed successfully.");
            } else {
                status.setStatus(SonarAnalysisState.FAILED);
                status.setMessage("Error executing SonarQube scanner.");
            }

        } catch (Exception e) {
            status.setStatus(SonarAnalysisState.FAILED);
            status.setMessage("Error during analysis: " + e.getMessage());
        } finally {
            status.setEndTime(System.currentTimeMillis());
            sonarAnalysisStatusRepository.save(status);
        }

        return CompletableFuture.completedFuture(sonarRepoAnalysisResult);
    }

    private boolean executeSonarScanner(Path projectPath, String projectKey, String projectName) {
        try {
            List<String> command = new ArrayList<>();
            command.add(sonarProperties.getScannerPath());
            command.add("-Dsonar.projectKey=" + projectKey);
            command.add("-Dsonar.projectName=" + projectName);
            command.add("-Dsonar.sources=.");
            command.add("-Dsonar.host.url=" + sonarProperties.getHostUrl());
            command.add("-Dsonar.token=" + sonarProperties.getToken());

            if (javaProjectCompiler.isJavaProject(projectPath)) {
                List<String> binaries = javaProjectCompiler.compileJavaProject(projectPath);
                String joined = String.join(",", binaries);
                command.add("-Dsonar.java.binaries=" + joined);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(projectPath.toString()));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            log.error("Error executing SonarQube scanner: {}", e.getMessage(), e);
            return false;
        }
    }

    private SonarRepoAnalysisResult getAndSaveResults(String repoAnalysisId, String projectKey) {
        int attempts = 0;
        while (attempts < MAX_DOWNLOAD_ATTEMPTS) {
            SonarRepoAnalysisResult result = sonarResultDownloader.fetchAnalysisResults(repoAnalysisId, projectKey);

            if (result != null && result.getComponents() != null && !result.getComponents().isEmpty()) {
                log.info("Successfully saved analysis results for project: {}", projectKey);
                sonarRepoAnalysisRepository.save(result);
                return result;
            }

            log.info("Failed to fetch analysis results for project: {}", projectKey);
            attempts++;

            try {
                Thread.sleep(DELAY_BETWEEN_DOWNLOAD_ATTEMPTS_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting to retry fetching analysis results: {}", e.getMessage(), e);
                break;
            }
        }

        log.error("Failed to fetch analysis results for project {} after {} attempts", projectKey, MAX_DOWNLOAD_ATTEMPTS);
        return null;
    }
}