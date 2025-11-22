package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.Commit;
import pwr.zpi.hotspotter.repositoryanalysis.logprocessing.model.FileChange;
import pwr.zpi.hotspotter.repositoryanalysis.util.AnalysisUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileInfoAnalyzer {

    private final FileInfoRepository fileInfoRepository;

    public FileInfoAnalyzerContext startAnalysis(String analysisId, Path repositoryPath, LocalDate referenceDate) {
        log.debug("Starting file info analysis for ID: {}", analysisId);
        Process clocProcess = startClocProcess(repositoryPath);
        return new FileInfoAnalyzerContext(analysisId, repositoryPath, referenceDate, clocProcess);
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

    public void finishAnalysis(FileInfoAnalyzerContext context) {
        if (context == null) return;

        log.debug("Finishing file info analysis for ID: {}", context.getAnalysisId());

        Set<String> existingFiles = AnalysisUtils.getExistingFileNames(context.getRepositoryPath());
        Map<String, FileLinesData> fileLinesData = getFileLinesDataFromProcess(context.getClocProcess());
        Collection<FileInfo> fileInfos = context.getFileInfos().values();

        List<FileInfo> fileInfosFiltered = fileInfos.stream()
                .filter(fileInfo -> existingFiles.contains(fileInfo.getFilePath()))
                .toList();

        fileInfosFiltered.forEach(fileInfo -> {
            calculateFileSize(fileInfo, context.getRepositoryPath());
            calculateCodeAge(fileInfo, context.getReferenceDate());
            addLinesData(fileInfo, fileLinesData);
        });

        try {
            AnalysisUtils.saveDataInBatches(fileInfoRepository, fileInfosFiltered);
            log.debug("Saved {} file info analysis data records for ID: {}", fileInfosFiltered.size(), context.getAnalysisId());
        } catch (Exception e) {
            log.error("Error saving file info data for analysis ID: {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private Process startClocProcess(Path repositoryPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c",
                    "cloc --by-file --unix --csv --quiet --skip-uniqueness --timeout 60 ."
            );
            pb.directory(repositoryPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            log.debug("Started cloc process for {}", repositoryPath);
            return process;
        } catch (IOException e) {
            log.error("Error starting cloc process for {}: {}", repositoryPath, e.getMessage(), e);
            return null;
        }
    }

    private Map<String, FileLinesData> getFileLinesDataFromProcess(Process process) {
        Map<String, FileLinesData> fileLinesData = new HashMap<>();
        if (process == null) return fileLinesData;

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            );

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if (line.startsWith("SUM,")) {
                    while (reader.readLine() != null) { }
                    break;
                }

                String[] parts = line.split(",", 5);
                if (parts.length >= 5) {
                    try {
                        String language = parts[0].trim();
                        String filePath = parts[1].trim().replace("./", "");
                        int blank = Integer.parseInt(parts[2].trim());
                        int comment = Integer.parseInt(parts[3].trim());
                        int code = Integer.parseInt(parts[4].trim());

                        FileLinesData data = new FileLinesData(language, code, comment, blank);
                        fileLinesData.put(filePath, data);

                    } catch (NumberFormatException _) { }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("cloc process exited with code {}", exitCode);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error reading cloc process output: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }

        return fileLinesData;
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

    private record FileLinesData(String language, int code, int comment, int blank) {
        public int total() {
            return code + comment + blank;
        }
    }

}
