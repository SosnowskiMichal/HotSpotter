package pwr.zpi.hotspotter.fileanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.common.sse.AnalysisSsePublisher;
import pwr.zpi.hotspotter.common.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.FileLogExtractor;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAnalysisService {

    private final FileLogExtractor fileLogExtractor;
    private final AnalysisSsePublisher ssePublisher;

    public void runFileAnalysis(
            RepositoryInfo repositoryInfo,
            AnalysisInfo analysisInfo,
            String filePath,
            SseEmitter emitter
    ) {
        try {
            executeFileAnalysis(repositoryInfo, analysisInfo, filePath, emitter);

            // TODO: Custom exception handling

        } catch (LogProcessingException e) {
            log.error("Log processing failed for file {}: {}", filePath, e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during file analysis for file {}: {}", filePath, e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } finally {
            emitter.complete();
        }
    }

    private void executeFileAnalysis(
            RepositoryInfo repositoryInfo,
            AnalysisInfo analysisInfo,
            String filePath,
            SseEmitter emitter
    ) {
        log.info("Starting analysis for file {} in repository: {}", filePath, repositoryInfo.getRemoteUrl());

        ssePublisher.sendProgress(emitter, AnalysisSseStatus.PROCESSING_DATA);

        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());
        List<FileCommit> fileCommits = fileLogExtractor.extractAndParseFileCommits(
                repositoryPath,
                filePath,
                analysisInfo.getStartDate(),
                analysisInfo.getEndDate()
        );

        // TODO: Implement

        log.info("Analysis completed for file {} in repository: {}", filePath, repositoryInfo.getRemoteUrl());
        ssePublisher.sendSuccess(emitter, analysisInfo.getId());

    }

}
