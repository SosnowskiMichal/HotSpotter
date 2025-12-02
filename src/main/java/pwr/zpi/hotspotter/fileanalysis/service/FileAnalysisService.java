package pwr.zpi.hotspotter.fileanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.common.exception.AnalysisException;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.common.sse.AnalysisSsePublisher;
import pwr.zpi.hotspotter.common.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;
import pwr.zpi.hotspotter.fileanalysis.complexity.service.FileComplexityService;
import pwr.zpi.hotspotter.fileanalysis.config.FileAnalysisConfig;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.FileLogExtractor;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;
import pwr.zpi.hotspotter.fileanalysis.versionextraction.FileVersionExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAnalysisService {

    private final AnalysisSsePublisher ssePublisher;
    private final FileAnalysisConfig fileAnalysisConfig;
    private final FileLogExtractor fileLogExtractor;
    private final FileVersionExtractor fileVersionExtractor;
    private final FileComplexityService fileComplexityService;

    public void runFileAnalysis(
            RepositoryInfo repositoryInfo,
            AnalysisInfo analysisInfo,
            String filePath,
            SseEmitter emitter
    ) {
        try {
            executeFileAnalysis(repositoryInfo, analysisInfo, filePath, emitter);

            // TODO: Custom exception handling

        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument in file analysis: {}", e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } catch (LogProcessingException e) {
            log.error("Log processing failed for file {}: {}", filePath, e.getMessage());
            ssePublisher.sendError(emitter, e.getMessage());

        } catch (AnalysisException e) {
            log.error("Analysis failed for file {}: {}", filePath, e.getMessage());
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
        Path outputPath = getOutputPath(analysisInfo.getId(), filePath);

        List<FileCommit> fileCommits = fileLogExtractor.extractAndParseFileCommits(
                repositoryPath,
                filePath,
                analysisInfo.getStartDate(),
                analysisInfo.getEndDate()
        );
        List<String> commitHashes = fileCommits.stream()
                .map(FileCommit::hash)
                .toList();

        try {
            fileVersionExtractor.extractFileVersions(repositoryPath, filePath, commitHashes, outputPath);

            ssePublisher.sendProgress(emitter, AnalysisSseStatus.ANALYZING);

            // TODO: Run cloc analysis on each file version and collect results

            // TODO: Calculate complexity for each file version and collect results
            String extension = FilenameUtils.getExtension(filePath);
            CompletableFuture<Map<String, FileComplexityReport>> complexityFuture =
                    fileComplexityService.analyze(outputPath, extension);

            // TODO: Collect information about methods and changes

            ssePublisher.sendProgress(emitter, AnalysisSseStatus.FINALIZING);

            // TODO: Save collected results to database
            Map<String, FileComplexityReport> complexityReports = complexityFuture.join();

            log.info("Analysis completed for file {} in repository: {}", filePath, repositoryInfo.getRemoteUrl());
            ssePublisher.sendSuccess(emitter, analysisInfo.getId());

        } finally {
            try {
                FileUtils.deleteDirectory(outputPath.toFile());
            } catch (IOException e) {
                log.warn("Could not delete directory {}", outputPath.toFile(), e);
            }
        }
    }

    private Path getOutputPath(String analysisId, String filePath) {
        String filePathHash = Integer.toHexString(filePath.hashCode());
        return Path.of(fileAnalysisConfig.getBaseDirectory(), "x-ray", analysisId, filePathHash);
    }

}
