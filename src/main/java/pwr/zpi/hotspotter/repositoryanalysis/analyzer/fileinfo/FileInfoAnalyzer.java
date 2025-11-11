package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return new FileInfoAnalyzerContext(analysisId, repositoryPath, referenceDate);

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

        Set<String> existingFiles = AnalysisUtils.getExistingFileNames(context.getRepositoryPath());
        Map<String, FileLinesData> fileLinesData = getFileLinesData(context.getRepositoryPath());
        Collection<FileInfo> fileInfos = context.getFileInfos().values();

        List<FileInfo> fileInfosFiltered = fileInfos.stream()
                .filter(fileInfo -> existingFiles.contains(fileInfo.getFilePath()))
                .toList();

        fileInfosFiltered.forEach(fileInfo -> {
            calculateCodeAge(fileInfo, context.getReferenceDate());
            addLinesData(fileInfo, fileLinesData);
        });

        try {
            AnalysisUtils.saveDataInBatches(fileInfoRepository, fileInfosFiltered);
        } catch (Exception e) {
            log.error("Error saving file info data for analysis ID {}: {}", context.getAnalysisId(), e.getMessage(), e);
        }
    }

    private Map<String, FileLinesData> getFileLinesData(Path repositoryPath) {
        Map<String, FileLinesData> fileLinesData = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "bash", "-c",
                    "cloc --by-file --unix --csv --quiet --skip-uniqueness ."
            );
            pb.directory(repositoryPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();

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
                    continue;
                }

                String[] parts = line.split(",", 5);
                if (parts.length >= 5) {
                    String language = parts[0].trim();
                    String filePath = parts[1].trim().replace("./", "");
                    int blank = Integer.parseInt(parts[2].trim());
                    int comment = Integer.parseInt(parts[3].trim());
                    int code = Integer.parseInt(parts[4].trim());

                    FileLinesData data = new FileLinesData(language, code, comment, blank);
                    fileLinesData.put(filePath, data);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("cloc process exited with code {} for {}", exitCode, repositoryPath);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error calculating file lines data for {} using cloc: {}", repositoryPath, e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        }

        return fileLinesData;
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
