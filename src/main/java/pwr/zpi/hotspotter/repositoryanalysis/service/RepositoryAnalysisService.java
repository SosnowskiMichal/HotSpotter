package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.OwnershipAnalyzer;
import pwr.zpi.hotspotter.fileanalysis.analyzer.ownership.OwnershipAnalyzerContext;
import pwr.zpi.hotspotter.repositoryanalysis.controller.RepositoryAnalysisController;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogParser;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisService {

    private final RepositoryManagementService repositoryManagementService;
    private final AnalysisInfoRepository analysisInfoRepository;
    private final LogExtractor logExtractor;
    private final LogParser logParser;
    private final ExecutorService executorService;

    // Inject all analyzers here
    private final OwnershipAnalyzer ownershipAnalyzer;

    public RepositoryAnalysisController.AnalysisResult runRepositoryAnalysis(String repositoryUrl, LocalDate startDate, LocalDate endDate) {
        long analysisStartTime = System.currentTimeMillis();

        RepositoryManagementService.RepositoryOperationResult result = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);
        if (!result.success()) {
            return RepositoryAnalysisController.AnalysisResult.failure("Could not clone or update repository: " + result.message());
        }

        RepositoryInfo repositoryInfo = result.repositoryInfo();
        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());

        AnalysisInfo analysisInfo = createAnalysisInfo(repositoryInfo, startDate, endDate);
        String analysisId = analysisInfo.getId();
        analysisInfoRepository.save(analysisInfo);

        LogExtractor.LogExtractionResult logExtractionResult = logExtractor.extractLogs(repositoryPath, analysisId, startDate, endDate);
        if (!logExtractionResult.success()) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            return RepositoryAnalysisController.AnalysisResult.failure("Log extraction failed: " + logExtractionResult.message());
        }

        Path logFilePath = logExtractionResult.logFilePath();
        LogParser.LogParsingResult logParsingResult = logParser.parseLogs(logFilePath);
        if (!logParsingResult.success()) {
            analysisInfo.markAsFailed();
            analysisInfoRepository.save(analysisInfo);
            logExtractor.deleteLogFile(logFilePath);
            return RepositoryAnalysisController.AnalysisResult.failure("Log parsing failed: " + logParsingResult.message());
        }

        try {
            OwnershipAnalyzerContext ownershipContext = ownershipAnalyzer.startAnalysis(analysisId, repositoryPath);

            try (Stream<Commit> commits = logParsingResult.commits()) {
                commits.forEach(commit -> {
                    ownershipAnalyzer.processCommit(commit, ownershipContext);
                });
            }

            ownershipAnalyzer.finishAnalysis(ownershipContext);

//            CompletableFuture.allOf(
//                    CompletableFuture.runAsync(() -> analyzer1.analyze(repositoryPath, analysisId), executorService),
//                    CompletableFuture.runAsync(() -> analyzer2.analyze(repositoryPath, analysisId), executorService)
//            ).join();

            long analysisEndTime = System.currentTimeMillis();
            long analysisDurationSeconds = (analysisEndTime - analysisStartTime) / 1000;

            analysisInfo.setAnalysisTimeInSeconds(analysisDurationSeconds);
            analysisInfo.markAsCompleted();
            analysisInfoRepository.save(analysisInfo);

        } finally {
            logExtractor.deleteLogFile(logFilePath);
        }

        log.info("Repository analysis completed for repository: {}, ID: {}", repositoryUrl, analysisId);
        return RepositoryAnalysisController.AnalysisResult.success("Analysis completed successfully.", analysisId);
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
