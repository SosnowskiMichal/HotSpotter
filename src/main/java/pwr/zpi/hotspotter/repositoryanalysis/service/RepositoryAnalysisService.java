package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.controller.RepositoryAnalysisController;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.LogExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepositoryAnalysisService {

    private final RepositoryManagementService repositoryManagementService;
    private final AnalysisInfoRepository analysisInfoRepository;
    private final LogExtractor logExtractor;
    private final ExecutorService executorService;

    // Inject all analyzers here

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

        try {
            CompletableFuture.allOf(
    //                CompletableFuture.runAsync(() -> analyzer1.analyze(repositoryPath, logFilePath, analysisId), executorService),
    //                CompletableFuture.runAsync(() -> analyzer2.analyze(repositoryPath, logFilePath, analysisId), executorService)
            ).join();

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
