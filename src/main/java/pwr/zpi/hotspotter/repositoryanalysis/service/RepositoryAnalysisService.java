package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.ActivityTrendsAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.ActivityTrendsContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.CouplingAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.coupling.CouplingAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.AnalysisStatisticsCalculator;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogParser;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.repositoryanalysis.sse.RepositoryAnalysisSsePublisher;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;
import pwr.zpi.hotspotter.sonar.service.SonarResultDownloader;
import pwr.zpi.hotspotter.sonar.service.SonarService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisService {

    private final RepositoryManagementService repositoryManagementService;
    private final AnalysisInfoRepository analysisInfoRepository;
    private final LogExtractor logExtractor;
    private final LogParser logParser;
    private final RepositoryAnalysisSsePublisher ssePublisher;
    private final SonarService sonarService;

    private final KnowledgeAnalyzer knowledgeAnalyzer;
    private final AuthorsAnalyzer authorsAnalyzer;
    private final FileInfoAnalyzer fileInfoAnalyzer;
    private final ActivityTrendsAnalyzer activityTrendsAnalyzer;
    private final CouplingAnalyzer couplingAnalyzer;
    private final AnalysisStatisticsCalculator analysisStatisticsCalculator;

    public void runRepositoryAnalysis(String repositoryUrl, LocalDate startDate, LocalDate endDate, SseEmitter emitter) {
        log.info("Starting analysis for repository: {}, time range: ({} - {})", repositoryUrl, startDate, endDate);
        LocalDateTime analysisStartedAt = LocalDateTime.now();

        RepositoryInfo repositoryInfo = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl, emitter);
        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());

        AnalysisInfo analysisInfo = createAnalysisInfo(repositoryInfo, startDate, endDate, analysisStartedAt);
        String analysisId = analysisInfo.getId();
        analysisInfoRepository.save(analysisInfo);

        Path logFilePath = null;
        try {
            ssePublisher.sendProgress(emitter, AnalysisSseStatus.PROCESSING_DATA);

            if (endDate != null) {
                restoreRepositoryToDate(repositoryPath, endDate);
            }

            CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarAnalysisFuture =
                    sonarService.runAnalysis(analysisId, repositoryPath, analysisId, repositoryInfo.getName());

            logFilePath = logExtractor.extractLogs(repositoryPath, analysisId, startDate, endDate);
            Stream<Commit> commits = logParser.parseLogs(logFilePath);

            ssePublisher.sendProgress(emitter, AnalysisSseStatus.ANALYZING);

            KnowledgeAnalyzerContext knowledgeContext = knowledgeAnalyzer.startAnalysis(analysisId, repositoryPath);
            AuthorsAnalyzerContext authorsContext = authorsAnalyzer.startAnalysis(analysisId, endDate);
            FileInfoAnalyzerContext fileInfoContext = fileInfoAnalyzer.startAnalysis(analysisId, repositoryPath, endDate);
            ActivityTrendsContext activityTrendsContext = activityTrendsAnalyzer.startAnalysis(analysisId, endDate, 6);
            CouplingAnalyzerContext couplingContext = couplingAnalyzer.startAnalysis(analysisId, repositoryPath, endDate);

            try (commits) {
                commits.forEach(commit -> {
                    knowledgeAnalyzer.processCommit(commit, knowledgeContext);
                    authorsAnalyzer.processCommit(commit, authorsContext);
                    fileInfoAnalyzer.processCommit(commit, fileInfoContext);
                    activityTrendsAnalyzer.processCommit(commit, activityTrendsContext);
                    couplingAnalyzer.processCommit(commit, couplingContext);
                });
            }

            ssePublisher.sendProgress(emitter, AnalysisSseStatus.FINALIZING);

            knowledgeAnalyzer.finishAnalysis(knowledgeContext);
            authorsAnalyzer.finishAnalysis(authorsContext);
            fileInfoAnalyzer.finishAnalysis(fileInfoContext);
            activityTrendsAnalyzer.finishAnalysis(activityTrendsContext);
            couplingAnalyzer.finishAnalysis(couplingContext);

            knowledgeAnalyzer.enrichAnalysisData(knowledgeContext);
            authorsAnalyzer.enrichAnalysisData(authorsContext);

            analysisStatisticsCalculator.calculateStatistics(analysisId);

            try {
                sonarAnalysisFuture.get();
            } catch (Exception e) {
                log.warn("Failed to retrieve SonarQube analysis results for analysis ID {}: {}", analysisId, e.getMessage());
            }

            analysisInfo.markAsCompleted();
            analysisInfoRepository.save(analysisInfo);

            log.info("Analysis completed for repository {}, ID: {}", repositoryUrl, analysisId);
            ssePublisher.sendSuccess(emitter, analysisId);

        } catch (LogProcessingException e) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            throw e;

        } catch (Exception e) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            throw new AnalysisException("Analysis failed: " + e.getMessage());

        } finally {
            if (logFilePath != null) {
                logExtractor.deleteLogFile(logFilePath);
            }
            if (endDate != null) {
                restoreRepositoryToLatest(repositoryPath);
            }
        }
    }

    private AnalysisInfo createAnalysisInfo(
            RepositoryInfo repositoryInfo,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime analysisStartedAt
    ) {
        String analysisId = UUID.randomUUID().toString();
        return AnalysisInfo.builder()
                .id(analysisId)
                .repositoryUrl(repositoryInfo.getRemoteUrl())
                .repositoryName(repositoryInfo.getName())
                .repositoryOwner(repositoryInfo.getOwner())
                .repositoryPlatform(repositoryInfo.getPlatform())
                .startDate(startDate)
                .endDate(endDate)
                .analysisStartedAt(analysisStartedAt)
                .build();
    }

    private void restoreRepositoryToDate(Path repositoryPath, LocalDate endDate) {
        log.debug("Restoring repository {} to {}", repositoryPath, endDate);
        LocalDate beforeDate = endDate.plusDays(1);

        try {
            int exitCode = executeGitCheckout(repositoryPath, beforeDate);
            if (exitCode != 0) {
                throw new AnalysisException("Failed to restore repository to date " + endDate);
            }
            log.debug("Repository {} restored to {}", repositoryPath, endDate);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new AnalysisException("Failed to restore repository to date " + endDate);
        }
    }

    private int executeGitCheckout(Path repositoryPath, LocalDate beforeDate) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c",
                "git checkout $(git rev-list -1 --before=\"" + beforeDate + "\" HEAD)"
        );
        pb.directory(repositoryPath.toFile());
        pb.redirectErrorStream(true);

        return executeProcess(pb);
    }

    private void restoreRepositoryToLatest(Path repositoryPath) {
        log.debug("Restoring repository {} to latest", repositoryPath);

        try {
            int exitCode = executeGitCheckoutLatest(repositoryPath);
            if (exitCode != 0) {
                log.error("Failed to restore repository {} to the latest state", repositoryPath);
            } else {
                log.debug("Repository {} restored to the latest state", repositoryPath);
            }

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("Failed to restore repository {} to the latest state", repositoryPath, e);
        }
    }

    private int executeGitCheckoutLatest(Path repositoryPath) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "bash", "-c",
                "git checkout $(git branch | grep -v '^\\*' | tr -d ' ')"
        );
        pb.directory(repositoryPath.toFile());
        pb.redirectErrorStream(true);

        return executeProcess(pb);
    }

    private int executeProcess(ProcessBuilder pb) throws IOException, InterruptedException {
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {}
        }

        return process.waitFor();
    }

}
