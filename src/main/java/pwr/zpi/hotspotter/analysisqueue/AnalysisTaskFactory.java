package pwr.zpi.hotspotter.analysisqueue;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.fileanalysis.service.FileAnalysisService;
import pwr.zpi.hotspotter.repositoryanalysis.exception.AnalysisException;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.repository.AnalysisInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.service.RepositoryAnalysisService;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTaskFactory {

    private final RepositoryAnalysisService repositoryAnalysisService;
    private final FileAnalysisService fileAnalysisService;
    private final AnalysisInfoRepository analysisInfoRepository;

    @Setter
    private ConcurrentHashMap<String, RepositoryInfo> repositoryInfoCache;

    public QueuedAnalysisTask createRepositoryAnalysisTask(
            String repositoryUrl,
            LocalDate startDate,
            LocalDate endDate,
            SseEmitter emitter
    ) {
        log.debug("Creating repository analysis task for: {}", repositoryUrl);

        Runnable analysisTask = () -> {
            RepositoryInfo repositoryInfo = repositoryInfoCache.get(repositoryUrl);
            if (repositoryInfo != null) {
                repositoryAnalysisService.runRepositoryAnalysis(repositoryInfo, startDate, endDate, emitter);
            } else {
                log.error("RepositoryInfo not found in cache for repository: {}", repositoryUrl);
                throw new AnalysisException("Repository information not available");
            }
        };

        return new QueuedAnalysisTask(repositoryUrl, endDate, emitter, analysisTask);
    }

    public QueuedAnalysisTask createFileAnalysisTask(String analysisId, String filePath, SseEmitter emitter) {
        log.debug("Creating file analysis task for analysisId: {}, file: {}", analysisId, filePath);

        AnalysisInfo analysisInfo = analysisInfoRepository.findById(analysisId)
                .orElseThrow(() -> {
                    log.error("Analysis not found with ID: {}", analysisId);
                    return new AnalysisException("Analysis with ID '" + analysisId + "' does not exist.");
                });

        if (analysisInfo.getStatus() != AnalysisInfo.AnalysisStatus.COMPLETED) {
            log.error("Analysis '{}' is not completed. Current status: {}", analysisId, analysisInfo.getStatus());
            throw new AnalysisException("Analysis with ID '" + analysisId + "' is not completed.");
        }

        LocalDate endDate = analysisInfo.getEndDate();

        Runnable analysisTask = () -> {
            RepositoryInfo repositoryInfo = repositoryInfoCache.get(analysisInfo.getRepositoryUrl());
            if (repositoryInfo != null) {
                fileAnalysisService.runFileAnalysis(repositoryInfo, analysisInfo, filePath, emitter);
            } else {
                log.error("RepositoryInfo not found in cache for analysisId: {}", analysisId);
                throw new AnalysisException("Repository information not available");
            }
        };

        return new QueuedAnalysisTask(analysisInfo.getRepositoryUrl(), endDate, emitter, analysisTask);
    }

}
