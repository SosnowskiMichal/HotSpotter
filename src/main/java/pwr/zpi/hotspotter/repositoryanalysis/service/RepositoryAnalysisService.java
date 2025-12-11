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
import pwr.zpi.hotspotter.common.exception.AnalysisException;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.common.exception.InvalidRepositoryUrlException;
import pwr.zpi.hotspotter.common.exception.RepositoryCloneException;
import pwr.zpi.hotspotter.common.exception.RepositoryUpdateException;
import pwr.zpi.hotspotter.repositoryanalysis.filter.AnalysisFileFilter;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.CommitStream;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.common.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.common.sse.AnalysisSsePublisher;
import pwr.zpi.hotspotter.common.util.AnalysisUtils;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.sonar.service.SonarResultDownloader;
import pwr.zpi.hotspotter.sonar.service.SonarService;
import pwr.zpi.hotspotter.user.model.User;

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

    private final AnalysisInfoRepository analysisInfoRepository;
    private final LogExtractor logExtractor;
    private final AnalysisSsePublisher ssePublisher;
    private final SonarService sonarService;
    private final AnalysisFileFilter analysisFileFilter;

    private final KnowledgeAnalyzer knowledgeAnalyzer;
    private final AuthorsAnalyzer authorsAnalyzer;
    private final FileInfoAnalyzer fileInfoAnalyzer;
    private final ActivityTrendsAnalyzer activityTrendsAnalyzer;
    private final CouplingAnalyzer couplingAnalyzer;
    private final AnalysisStatisticsCalculator analysisStatisticsCalculator;

    public void runRepositoryAnalysis(
            RepositoryInfo repositoryInfo,
            LocalDate startDate,
            LocalDate endDate,
            SseEmitter emitter,
            User user
    ) {
        try {
            executeRepositoryAnalysis(repositoryInfo, startDate, endDate, emitter, user);

        } catch (InvalidRepositoryUrlException e) {
            log.warn("Invalid repository URL {}: {}", repositoryInfo.getRemoteUrl(), e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } catch (RepositoryCloneException | RepositoryUpdateException e) {
            log.error("Repository operation failed for {}: {}", repositoryInfo.getRemoteUrl(), e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } catch (LogProcessingException e) {
            log.error("Log processing failed for repository {}: {}", repositoryInfo.getRemoteUrl(), e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } catch (AnalysisException e) {
            log.error("Analysis failed for repository {}: {}", repositoryInfo.getRemoteUrl(), e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during analysis of repository {}: {}", repositoryInfo.getRemoteUrl(), e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } finally {
            emitter.complete();
        }
    }

    private void executeRepositoryAnalysis(
            RepositoryInfo repositoryInfo,
            LocalDate startDate,
            LocalDate endDate,
            SseEmitter emitter,
            User user
    ) {
        log.info("Starting analysis for repository: {}, time range: ({} - {})", repositoryInfo.getRemoteUrl(), startDate, endDate);

        ssePublisher.sendProgress(emitter, AnalysisSseStatus.PROCESSING_DATA);

        LocalDateTime analysisStartedAt = LocalDateTime.now();
        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());

        AnalysisInfo analysisInfo = createAnalysisInfo(repositoryInfo, startDate, endDate, analysisStartedAt, user);
        String analysisId = analysisInfo.getId();
        analysisInfoRepository.save(analysisInfo);

        ssePublisher.sendProgress(emitter, AnalysisSseStatus.ANALYZING);

        try {
            CompletableFuture<SonarResultDownloader.SonarAnalysisResults> sonarAnalysisFuture =
                    sonarService.runAnalysis(analysisId, repositoryPath, analysisId, repositoryInfo.getName());

            KnowledgeAnalyzerContext knowledgeContext = knowledgeAnalyzer.startAnalysis(analysisId, repositoryPath);
            AuthorsAnalyzerContext authorsContext = authorsAnalyzer.startAnalysis(analysisId, endDate);
            FileInfoAnalyzerContext fileInfoContext = fileInfoAnalyzer.startAnalysis(analysisId, repositoryPath, endDate);
            ActivityTrendsContext activityTrendsContext = activityTrendsAnalyzer.startAnalysis(analysisId, endDate);
            CouplingAnalyzerContext couplingContext = couplingAnalyzer.startAnalysis(analysisId, repositoryPath, endDate);

            try (CommitStream commitStream = logExtractor.extractAndParseCommits(repositoryPath, startDate, endDate)) {
                try (Stream<Commit> commits = commitStream.getStream()) {
                    commits.forEach(commit -> {
                        Commit filteredCommit = analysisFileFilter.filterCommit(commit);
                        knowledgeAnalyzer.processCommit(filteredCommit, knowledgeContext);
                        authorsAnalyzer.processCommit(filteredCommit, authorsContext);
                        fileInfoAnalyzer.processCommit(filteredCommit, fileInfoContext);
                        activityTrendsAnalyzer.processCommit(filteredCommit, activityTrendsContext);
                        couplingAnalyzer.processCommit(filteredCommit, couplingContext);
                    });
                }
            }

            try {
                sonarAnalysisFuture.get();
            } catch (Exception e) {
                log.warn("Failed to retrieve SonarQube analysis results for analysis ID {}: {}", analysisId, e.getMessage());
            }

            ssePublisher.sendProgress(emitter, AnalysisSseStatus.FINALIZING);

            knowledgeAnalyzer.finishAnalysis(knowledgeContext);
            authorsAnalyzer.finishAnalysis(authorsContext);
            fileInfoAnalyzer.finishAnalysis(fileInfoContext, analysisInfo);
            activityTrendsAnalyzer.finishAnalysis(activityTrendsContext);
            couplingAnalyzer.finishAnalysis(couplingContext);

            knowledgeAnalyzer.enrichAnalysisData(knowledgeContext);
            authorsAnalyzer.enrichAnalysisData(authorsContext);

            analysisStatisticsCalculator.calculateStatistics(analysisId);

            setStartDateFromFirstCommitIfEmpty(analysisInfo, activityTrendsContext.getFirstCommitDate());

            analysisInfo.markAsCompleted();
            analysisInfoRepository.save(analysisInfo);

            log.info("Analysis completed for repository {}, ID: {}", repositoryInfo.getRemoteUrl(), analysisId);
            ssePublisher.sendSuccess(emitter, analysisId);

        } catch (Exception e) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            throw e;
        }
    }

    private AnalysisInfo createAnalysisInfo(
            RepositoryInfo repositoryInfo,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime analysisStartedAt,
            User user
    ) {
        String analysisId = UUID.randomUUID().toString();
        LocalDate nonNullEndDate = endDate != null ? endDate : LocalDate.now();

        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());
        String lastCommitHash = AnalysisUtils.getLastCommitHash(repositoryPath);

        return AnalysisInfo.builder()
                .id(analysisId)
                .userId(user != null ? user.getId() : null)
                .repositoryUrl(repositoryInfo.getRemoteUrl())
                .repositoryName(repositoryInfo.getName())
                .repositoryOwner(repositoryInfo.getOwner())
                .repositoryPlatform(repositoryInfo.getPlatform())
                .startDate(startDate)
                .endDate(nonNullEndDate)
                .lastCommitHash(lastCommitHash)
                .analysisStartedAt(analysisStartedAt)
                .build();
    }

    private void setStartDateFromFirstCommitIfEmpty(AnalysisInfo analysisInfo, LocalDate firstCommitDate) {
        if (analysisInfo.getStartDate() == null && firstCommitDate != null) {
            analysisInfo.setStartDate(firstCommitDate);
        }
    }

}
