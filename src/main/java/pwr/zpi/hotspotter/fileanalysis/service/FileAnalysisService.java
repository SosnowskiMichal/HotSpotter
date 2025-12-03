package pwr.zpi.hotspotter.fileanalysis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pwr.zpi.hotspotter.common.cloc.ClocService;
import pwr.zpi.hotspotter.common.cloc.model.FileLinesData;
import pwr.zpi.hotspotter.common.exception.AnalysisException;
import pwr.zpi.hotspotter.common.exception.LogProcessingException;
import pwr.zpi.hotspotter.common.sse.AnalysisSsePublisher;
import pwr.zpi.hotspotter.common.sse.AnalysisSseStatus;
import pwr.zpi.hotspotter.fileanalysis.blame.FileBlameExtractor;
import pwr.zpi.hotspotter.fileanalysis.blame.model.FileAuthorStatistics;
import pwr.zpi.hotspotter.fileanalysis.complexity.model.FileComplexityReport;
import pwr.zpi.hotspotter.fileanalysis.complexity.service.FileComplexityService;
import pwr.zpi.hotspotter.fileanalysis.config.FileAnalysisConfig;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.FileLogExtractor;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;
import pwr.zpi.hotspotter.fileanalysis.model.FileAnalysisResult;
import pwr.zpi.hotspotter.fileanalysis.model.FileVersionStatistics;
import pwr.zpi.hotspotter.fileanalysis.repository.FileAnalysisResultRepository;
import pwr.zpi.hotspotter.fileanalysis.versionextraction.FileVersionExtractor;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositorymanagement.model.RepositoryInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileAnalysisService {

    private final AnalysisSsePublisher ssePublisher;
    private final FileAnalysisConfig fileAnalysisConfig;
    private final FileLogExtractor fileLogExtractor;
    private final FileVersionExtractor fileVersionExtractor;
    private final FileComplexityService fileComplexityService;
    private final ClocService clocService;
    private final FileBlameExtractor fileBlameExtractor;
    private final FileAnalysisResultRepository fileAnalysisResultRepository;

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

        FileAnalysisResult fileAnalysisResult = createFileAnalysisResult(analysisInfo.getId(), filePath);
        Path repositoryPath = Path.of(repositoryInfo.getLocalPath());
        Path outputPath = getOutputPath(analysisInfo.getId(), filePath);
        Path currentFilePath = repositoryPath.resolve(filePath);
        String fileExtension = FilenameUtils.getExtension(filePath);

        List<FileCommit> fileCommits = fileLogExtractor.extractAndParseFileCommits(
                repositoryPath,
                filePath,
                analysisInfo.getStartDate(),
                analysisInfo.getEndDate()
        );

        fileAnalysisResult.setFileCommits(fileCommits);
        fileAnalysisResult.setTotalFileVersions(fileCommits.size());

        try {
            fileVersionExtractor.extractFileVersions(repositoryPath, fileCommits, outputPath);

            ssePublisher.sendProgress(emitter, AnalysisSseStatus.ANALYZING);

            CompletableFuture<Map<String, FileComplexityReport>> complexityFuture =
                    fileComplexityService.analyze(outputPath, fileExtension);
            CompletableFuture<Map<String, FileLinesData>> clocFuture = clocService.analyzeDirectory(outputPath);

            List<FileAuthorStatistics> currentAuthors = fileBlameExtractor.extractCurrentAuthors(currentFilePath);

            // TODO: Collect information about methods and changes

            Map<String, FileLinesData> clocResults = collectFutureResults(clocFuture, "cloc");
            Map<String, FileComplexityReport> complexityResults = collectFutureResults(complexityFuture, "complexity");

            ssePublisher.sendProgress(emitter, AnalysisSseStatus.FINALIZING);

            // TODO: Combine results
            combineClocResults(fileAnalysisResult, clocResults);
            combineComplexityResults(fileAnalysisResult, complexityResults);
            combineCurrentAuthorsResults(fileAnalysisResult, currentAuthors);

            fileAnalysisResult.markAsCompleted();
            fileAnalysisResultRepository.save(fileAnalysisResult);

            log.info("Analysis completed for file {} in repository: {}", filePath, repositoryInfo.getRemoteUrl());
            ssePublisher.sendSuccess(emitter, analysisInfo.getId());

        } finally {
            try {
                cleanupOutputDirectory(outputPath);
            } catch (IOException e) {
                log.warn("Could not delete directory {}", outputPath, e);
            }
        }
    }

    private FileAnalysisResult createFileAnalysisResult(String analysisId, String filePath) {
        return FileAnalysisResult.builder()
                .analysisId(analysisId)
                .filePath(filePath)
                .build();
    }

    private Path getOutputPath(String analysisId, String filePath) {
        String filePathHash = Integer.toHexString(filePath.hashCode());
        return Path.of(fileAnalysisConfig.getBaseDirectory(), "x-ray", analysisId, filePathHash);
    }

    private <T> Map<String, T> collectFutureResults(CompletableFuture<Map<String, T>> future, String resultType) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving {} results: {}", resultType, e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return new HashMap<>();
        }
    }

    private void combineClocResults(
            FileAnalysisResult fileAnalysisResult,
            Map<String, FileLinesData> clocResults
    ) {
        String fileExtension = FilenameUtils.getExtension(fileAnalysisResult.getFilePath());
        List<FileVersionStatistics> statistics = new ArrayList<>();

        for (FileCommit commit : fileAnalysisResult.getFileCommits()) {
            String fileName = commit.hash() + "." + fileExtension;
            FileLinesData fileLinesData = clocResults.get(fileName);

            FileVersionStatistics.FileVersionStatisticsBuilder builder = FileVersionStatistics.builder()
                    .hash(commit.hash())
                    .date(commit.date())
                    .path(commit.path());

            if (fileLinesData != null) {
                builder.totalLines(fileLinesData.total())
                        .codeLines(fileLinesData.code())
                        .commentLines(fileLinesData.comment())
                        .blankLines(fileLinesData.blank());
            }

            statistics.add(builder.build());
        }

        fileAnalysisResult.setFileVersionStatistics(statistics);
    }

    private void combineComplexityResults(
            FileAnalysisResult fileAnalysisResult,
            Map<String, FileComplexityReport> complexityResults
    ) {
        List<FileVersionStatistics> statistics = fileAnalysisResult.getFileVersionStatistics();
        if (statistics == null || statistics.isEmpty()) return;

        for (FileVersionStatistics stats : statistics) {
            FileComplexityReport report = complexityResults.get(stats.getHash());

            if (report != null) {
                stats.setComplexity(report.getTotalCCN());
                stats.setNumberOfMethods(report.getMethodsCount());
            }
        }
    }

    private void combineCurrentAuthorsResults(
            FileAnalysisResult fileAnalysisResult,
            List<FileAuthorStatistics> currentAuthors
    ) {
        fileAnalysisResult.setCurrentAuthors(currentAuthors);
        fileAnalysisResult.setNumberOfCurrentAuthors(currentAuthors.size());
    }

    private void cleanupOutputDirectory(Path outputPath) throws IOException {
        FileUtils.deleteDirectory(outputPath.toFile());

        Path parentPath = outputPath.getParent();
        if (parentPath != null && parentPath.toFile().exists()) {
            if (FileUtils.isEmptyDirectory(parentPath.toFile())) {
                FileUtils.deleteDirectory(parentPath.toFile());
                log.debug("Deleted empty parent directory: {}", parentPath);
            }
        }
    }

}
