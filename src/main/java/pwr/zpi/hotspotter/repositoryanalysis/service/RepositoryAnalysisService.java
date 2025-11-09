package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.FileInfoAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.knowledge.KnowledgeAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzer;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.AuthorsAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.exception.LogProcessingException;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogParser;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.sse.RepositoryAnalysisSsePublisher;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisService {

    private final RepositoryManagementService repositoryManagementService;
    private final AnalysisInfoRepository analysisInfoRepository;
    private final LogExtractor logExtractor;
    private final LogParser logParser;
    private final RepositoryAnalysisSsePublisher sse;

    // Inject all analyzers here
    private final KnowledgeAnalyzer knowledgeAnalyzer;
    private final AuthorsAnalyzer authorsAnalyzer;
    private final FileInfoAnalyzer fileInfoAnalyzer;

    public void runRepositoryAnalysis(String repositoryUrl, LocalDate startDate, LocalDate endDate, SseEmitter emitter) {
        long analysisStartTime = System.currentTimeMillis();

        RepositoryInfo repositoryInfo = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);
        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());

        AnalysisInfo analysisInfo = createAnalysisInfo(repositoryInfo, startDate, endDate);
        String analysisId = analysisInfo.getId();
        analysisInfoRepository.save(analysisInfo);

        Path logFilePath = null;
        try {
            sse.sendProgress(emitter, AnalysisInfo.AnalysisSseStatus.PROCESSING_DATA);
            logFilePath = logExtractor.extractLogs(repositoryPath, analysisId, startDate, endDate);
            Stream<Commit> commits = logParser.parseLogs(logFilePath);

            sse.sendProgress(emitter, AnalysisInfo.AnalysisSseStatus.ANALYZING);
            KnowledgeAnalyzerContext knowledgeContext = knowledgeAnalyzer.startAnalysis(analysisId, repositoryPath);
            AuthorsAnalyzerContext authorsContext = authorsAnalyzer.startAnalysis(analysisId, endDate);
            FileInfoAnalyzerContext fileInfoContext = fileInfoAnalyzer.startAnalysis(analysisId, repositoryPath, endDate);

            try (commits) {
                commits.forEach(commit -> {
                    knowledgeAnalyzer.processCommit(commit, knowledgeContext);
                    authorsAnalyzer.processCommit(commit, authorsContext);
                    fileInfoAnalyzer.processCommit(commit, fileInfoContext);
                });
            }

            sse.sendProgress(emitter, AnalysisInfo.AnalysisSseStatus.GENERATING_RESULTS);
            knowledgeAnalyzer.finishAnalysis(knowledgeContext);
            authorsAnalyzer.finishAnalysis(authorsContext);
            fileInfoAnalyzer.finishAnalysis(fileInfoContext);

            sse.sendProgress(emitter, AnalysisInfo.AnalysisSseStatus.FINALIZING);
            knowledgeAnalyzer.enrichAnalysisData(knowledgeContext);
            authorsAnalyzer.enrichAnalysisData(authorsContext);

            long analysisEndTime = System.currentTimeMillis();
            long analysisDurationSeconds = (analysisEndTime - analysisStartTime) / 1000;

            analysisInfo.setAnalysisTimeInSeconds(analysisDurationSeconds);
            analysisInfo.markAsCompleted();
            sse.sendProgress(emitter, AnalysisInfo.AnalysisSseStatus.COMPLETED);
            analysisInfoRepository.save(analysisInfo);

            log.info("Analysis completed for repository {} in {} seconds, ID: {}",
                    repositoryUrl, analysisDurationSeconds, analysisId);
            sse.sendComplete(emitter, analysisId);

        } catch (LogProcessingException e) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            sse.sendError(emitter, e.getMessage());
            throw e;

        } catch (Exception e) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            log.error("Unexpected error during analysis for repository {}: {}", repositoryUrl, e.getMessage(), e);
            sse.sendError(emitter, e.getMessage());
            throw new AnalysisException("Analysis failed: " + e.getMessage());

        } finally {
            if (logFilePath != null) {
                logExtractor.deleteLogFile(logFilePath);
            }
        }
    }

    private AnalysisInfo createAnalysisInfo(RepositoryInfo repositoryInfo, LocalDate startDate, LocalDate endDate) {
        String analysisId = UUID.randomUUID().toString();
        return AnalysisInfo.builder()
                .id(analysisId)
                .repositoryUrl(repositoryInfo.getRemoteUrl())
                .repositoryName(repositoryInfo.getName())
                .repositoryOwner(repositoryInfo.getOwner())
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

}
