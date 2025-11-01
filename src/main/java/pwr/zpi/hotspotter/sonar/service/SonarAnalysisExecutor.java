package pwr.zpi.hotspotter.sonar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.exceptions.ObjectNotFoundException;
import pwr.zpi.hotspotter.sonar.config.SonarProperties;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisState;
import pwr.zpi.hotspotter.sonar.model.analysisstatus.SonarAnalysisStatus;
import pwr.zpi.hotspotter.sonar.model.repoanalysis.SonarRepoAnalysisResult;
import pwr.zpi.hotspotter.sonar.repository.SonarAnalysisStatusRepository;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SonarAnalysisExecutor {
    private final static int MILLISECONDS_TO_WAIT_BEFORE_FETCHING_RESULTS = 10000;

    private final SonarAnalysisStatusRepository sonarAnalysisStatusRepository;
    private final SonarResultDownloader sonarResultDownloader;
    private final JavaProjectCompiler javaProjectCompiler;
    private final SonarProperties sonarProperties;


    @Async("sonarQubeAnalysisExecutor")
    public void runAnalysisAsync(String analysisId, String projectPath, String projectKey, String projectName) {
        SonarAnalysisStatus status = sonarAnalysisStatusRepository.findById(analysisId).orElseThrow(() ->
                new ObjectNotFoundException("SonarQube analysis status not found for ID: " + analysisId));

        try {
            status.setStatus(SonarAnalysisState.RUNNING);
            status.setMessage("SonarQube analysis is running.");
            sonarAnalysisStatusRepository.save(status);

            boolean success = executeSonarScanner(projectPath, projectKey, projectName);

            if (success) {
                Thread.sleep(MILLISECONDS_TO_WAIT_BEFORE_FETCHING_RESULTS);
                SonarRepoAnalysisResult saveResult = saveResults(status.getProjectKey());
                if (saveResult == null) {
                    throw new RuntimeException("Failed to fetch/save analysis results for project: " + status.getProjectKey());
                }

                status.setRepoAnalysisId(saveResult.getId());
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
    }

    private boolean executeSonarScanner(String projectPath, String projectKey, String projectName) {
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
            processBuilder.directory(new File(projectPath));
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            log.error("Error executing SonarQube scanner: {}", e.getMessage(), e);
            return false;
        }
    }

    private SonarRepoAnalysisResult saveResults(String projectKey) {
        return sonarResultDownloader.fetchAndSaveAnalysisResults(projectKey);
    }
}