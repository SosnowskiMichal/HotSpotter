package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.common.cloc.ClocService;
import pwr.zpi.hotspotter.common.cloc.model.FileLinesData;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.filter.AnalysisFileFilter;
import pwr.zpi.hotspotter.repositoryanalysis.model.AnalysisInfo;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;
import pwr.zpi.hotspotter.repositoryanalysis.util.RepositoryFileUrlBuilder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileInfoAnalyzer {

    private final FileInfoRepository fileInfoRepository;
    private final AnalysisFileFilter analysisFileFilter;
    private final ClocService clocService;

    public FileInfoAnalyzerContext startAnalysis(String analysisId, Path repositoryPath, LocalDate referenceDate) {
        log.debug("Starting file info analysis for ID: {}", analysisId);
        CompletableFuture<Map<String, FileLinesData>> clocFuture = clocService.analyzeDirectory(repositoryPath);
        return new FileInfoAnalyzerContext(analysisId, repositoryPath, referenceDate, clocFuture);
    }

    public void processCommit(Commit commit, FileInfoAnalyzerContext context) {
        if (commit == null || context == null) return;

        LocalDate date = commit.getCommitDateAsLocalDate();

        for (FileChange fileChange : commit.changedFiles()) {
            String filePath = fileChange.filePath();

            if (fileChange.isRenamed()) {
                context.updateFilePath(fileChange.oldPath(), fileChange.newPath());
                filePath = fileChange.newPath();
            }

            context.recordContribution(filePath, date);
        }
    }

    public void finishAnalysis(FileInfoAnalyzerContext context, AnalysisInfo analysisInfo) {
        if (context == null || analysisInfo == null) return;

        log.debug("Finishing file info analysis for ID: {}", context.getAnalysisId());

        Set<String> existingFiles = AnalysisUtils.getFilteredExistingFileNames(context.getRepositoryPath(), analysisFileFilter);

        // TODO: Verify
        Map<String, FileLinesData> fileLinesData;
        try {
            fileLinesData = context.getClocFuture().get(120, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("Cloc analysis timed out for analysis {}", context.getAnalysisId());
            fileLinesData = new HashMap<>();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error retrieving cloc results for analysis {}: {}", context.getAnalysisId(), e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            fileLinesData = new HashMap<>();
        }

        Collection<FileInfo> fileInfos = context.getFileInfos().values();

        List<FileInfo> fileInfosFiltered = fileInfos.stream()
                .filter(fileInfo -> existingFiles.contains(fileInfo.getFilePath()))
                .toList();

        for (FileInfo fileInfo : fileInfosFiltered) {
            calculateFileSize(fileInfo, context.getRepositoryPath());
            calculateCodeAge(fileInfo, context.getReferenceDate());
            addLinesData(fileInfo, fileLinesData);
            addFileUrl(fileInfo, analysisInfo);
        }

        try {
            AnalysisUtils.saveDataInBatches(fileInfoRepository, fileInfosFiltered);
            log.debug("Saved {} file info analysis data records for ID: {}", fileInfosFiltered.size(), context.getAnalysisId());
        } catch (Exception e) {
            log.error("Error saving file info data for analysis ID: {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private void calculateFileSize(FileInfo fileInfo, Path repositoryPath) {
        Path filePath = repositoryPath.resolve(fileInfo.getFilePath());
        long fileSizeInBytes = FileUtils.sizeOf(filePath.toFile());
        String fileSizeStr = FileUtils.byteCountToDisplaySize(fileSizeInBytes).replace("bytes", "B");
        fileInfo.setFileSize(fileSizeStr);
    }

    private void calculateCodeAge(FileInfo fileInfo, LocalDate referenceDate) {
        LocalDate lastCommitDate = fileInfo.getLastCommitDate();

        int codeAgeDays = (int) ChronoUnit.DAYS.between(lastCommitDate, referenceDate);
        int codeAgeMonths = (int) ChronoUnit.MONTHS.between(lastCommitDate, referenceDate);

        fileInfo.setCodeAgeDays(codeAgeDays);
        fileInfo.setCodeAgeMonths(codeAgeMonths);
    }

    private void addLinesData(FileInfo fileInfo, Map<String, FileLinesData> fileLinesData) {
        FileLinesData linesData = fileLinesData.get(fileInfo.getFilePath());
        if (linesData != null) {
            fileInfo.setFileType(linesData.language());
            fileInfo.setCodeLines(linesData.code());
            fileInfo.setCommentLines(linesData.comment());
            fileInfo.setBlankLines(linesData.blank());
            fileInfo.setTotalLines(linesData.total());
        }
    }

    private void addFileUrl(FileInfo fileInfo, AnalysisInfo analysisInfo) {
        String fileUrl = RepositoryFileUrlBuilder.buildFileUrl(
            analysisInfo.getRepositoryPlatform(),
            analysisInfo.getRepositoryUrl(),
            fileInfo.getFilePath(),
            analysisInfo.getLastCommitHash()
        );
        fileInfo.setFileUrl(fileUrl);
    }

}
