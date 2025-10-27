package pwr.zpi.hotspotter.repositoryanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pwr.zpi.hotspotter.repositoryanalysis.controller.RepositoryAnalysisController;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryManagementService;
import pwr.zpi.hotspotter.repositorymanagement.service.RepositoryOperationResult;

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
    private final ExecutorService executorService;

    // Inject all analyzers here

    public RepositoryAnalysisController.AnalysisResult runRepositoryAnalysis(
            String repositoryUrl,
            LocalDate afterDate,
            LocalDate beforeDate) {

        RepositoryOperationResult result = repositoryManagementService.cloneOrUpdateRepository(repositoryUrl);
        if (!result.success()) {
            return RepositoryAnalysisController.AnalysisResult
                    .failure("Could not clone or update repository: " + result.message());
        }

        RepositoryInfo repositoryInfo = result.repositoryInfo();
        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());

        AnalysisInfo analysisInfo = createAnalysisInfo(repositoryInfo, afterDate, beforeDate);
        String analysisId = analysisInfo.getId();
        analysisInfoRepository.save(analysisInfo);

        CompletableFuture.allOf(
//                CompletableFuture.runAsync(() -> analyzer.analyze(repositoryPath, analysisId), executorService),
//                CompletableFuture.runAsync(() -> analyzer.analyze(repositoryPath, analysisId), executorService)
        ).join();

        analysisInfo.markAsCompleted();
        analysisInfoRepository.save(analysisInfo);

        log.info("Repository analysis completed for repository: {}, ID: {}", repositoryUrl, analysisId);
        return RepositoryAnalysisController.AnalysisResult.success(analysisId);
    }

    private AnalysisInfo createAnalysisInfo(RepositoryInfo repositoryInfo, LocalDate afterDate, LocalDate beforeDate) {
        String analysisId = UUID.randomUUID().toString();
        return AnalysisInfo.builder()
                .id(analysisId)
                .repositoryUrl(repositoryInfo.getRemoteUrl())
                .repositoryName("")
                .repositoryOwner("")
                .afterDate(afterDate)
                .beforeDate(beforeDate)
                .build();
    }

}
