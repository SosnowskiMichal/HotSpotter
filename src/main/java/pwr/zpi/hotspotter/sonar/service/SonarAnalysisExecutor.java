package pwr.zpi.hotspotter.sonar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.common.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisState;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.fileanalysis.SonarFileAnalysisResult;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarFileAnalysisRepository;
import pwr.zpi.hotspotter.sonar.repository.SonarRepoAnalysisRepository;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final SonarFileAnalysisRepository sonarFileAnalysisRepository;


    @Async("sonarExecutor")
    public CompletableFuture<Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult>> runAnalysisAsync(String repoAnalysisId, String sonarAnalysisId, Path projectPath, String projectKey, String projectName) {
        SonarAnalysisStatus status = sonarAnalysisStatusRepository.findById(sonarAnalysisId).orElseThrow(() ->
                new ObjectNotFoundException("SonarQube analysis status not found for ID: " + sonarAnalysisId));

        Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult> sonarAnalysisResult = null;

        try {
            status.setStatus(SonarAnalysisState.RUNNING);
            status.setMessage("SonarQube analysis is running.");
            log.info("SonarQube analysis is running.");
            sonarAnalysisStatusRepository.save(status);

            boolean success = executeSonarScanner(projectPath, projectKey, projectName);

            if (success) {
                Thread.sleep(MILLISECONDS_TO_WAIT_BEFORE_FETCHING_RESULTS);
                sonarAnalysisResult = getAndSaveResults(repoAnalysisId, status.getProjectKey());
                if (sonarAnalysisResult == null) {
                    throw new RuntimeException("Failed to fetch/save analysis results for project: " + status.getProjectKey());
                }

                status.setStatus(SonarAnalysisState.SUCCESS);
                status.setMessage("SonarQube analysis completed successfully.");
                log.info("SonarQube analysis completed successfully for project: {}", status.getProjectKey());
            } else {
                status.setStatus(SonarAnalysisState.FAILED);
                status.setMessage("Error executing SonarQube scanner.");
                log.error("Error executing SonarQube scanner for project: {}", status.getProjectKey());
            }

        } catch (Exception e) {
            status.setStatus(SonarAnalysisState.FAILED);
            status.setMessage("Error during analysis: " + e.getMessage());
            log.error("Error during SonarQube analysis for project {}: {}", status.getProjectKey(), e.getMessage(), e);
        } finally {
            status.setEndTime(LocalDateTime.now());
            sonarAnalysisStatusRepository.save(status);
        }

        return CompletableFuture.completedFuture(sonarAnalysisResult);
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

            Optional<Path> commonJavaSourceRoot = javaProjectCompiler.findCommonJavaSourceRoot(projectPath);
            if (commonJavaSourceRoot.isPresent()) {
                try {
                    List<String> binaries = javaProjectCompiler.compileJavaProject(commonJavaSourceRoot.get());
                    String joined = String.join(",", binaries);
                    command.add("-Dsonar.java.binaries=" + joined);
                } catch (Exception e) {
                    log.error("Error compiling Java project at {}: {}", commonJavaSourceRoot.get(), e.getMessage());
                    command.add("-Dsonar.exclusions=**/*.java");
                }
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File(projectPath.toString()));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            if (sonarProperties.isLogSonarOutput()) {
                logSonarOutput(process);
            }

            int exitCode = process.waitFor();
            boolean success = exitCode == 0;
            if (!success) {
                log.error("SonarQube scanner exited with code: {}", exitCode);
            }

            return success;

        } catch (Exception e) {
            log.error("Error executing SonarQube scanner: {}", e.getMessage(), e);
            return false;
        }
    }

    private static void logSonarOutput(Process process) {
        new Thread(() -> {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[SonarQube Scanner] {}", line);
                }
            } catch (Exception e) {
                log.error("Error reading SonarQube scanner output: {}", e.getMessage(), e);
            }
        }).start();
    }

    private Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult> getAndSaveResults(String repoAnalysisId, String projectKey) {
        int attempts = 0;
        while (attempts < MAX_DOWNLOAD_ATTEMPTS) {
            Pair<SonarRepoAnalysisResult, SonarFileAnalysisResult> result = sonarResultDownloader.fetchAnalysisResults(repoAnalysisId, projectKey);

            if (result != null && result.getFirst().getComponents() != null && !result.getFirst().getComponents().isEmpty()) {
                SonarRepoAnalysisResult repoAnalysisResult = result.getFirst();
                SonarFileAnalysisResult sonarFileAnalysisResult = result.getSecond();
                sonarRepoAnalysisRepository.save(repoAnalysisResult);
                sonarFileAnalysisRepository.save(sonarFileAnalysisResult);
                log.info("Successfully saved analysis results for project: {}", projectKey);

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