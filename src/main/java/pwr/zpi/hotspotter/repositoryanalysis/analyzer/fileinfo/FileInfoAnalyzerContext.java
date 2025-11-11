package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo;

import lombok.Getter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
public class FileInfoAnalyzerContext {

    // TODO: Get from user settings
    private static final int HOT_SPOT_ANALYSIS_PERIOD_MONTHS = 6;

    private final String analysisId;
    private final Path repositoryPath;
    private final LocalDate referenceDate;
    private final Map<String, FileInfo> fileInfos;

    public FileInfoAnalyzerContext(String analysisId, Path repositoryPath, LocalDate referenceDate) {
        this.analysisId = analysisId;
        this.repositoryPath = repositoryPath;
        this.referenceDate = referenceDate != null ? referenceDate : LocalDate.now();
        this.fileInfos = new HashMap<>();
    }

    public void recordContribution(String filePath, LocalDate date) {
        fileInfos.compute(filePath, (_, fileInfo) -> {
            if (fileInfo == null) {
                fileInfo = FileInfo.builder()
                        .analysisId(analysisId)
                        .filePath(filePath)
                        .fileName(getFileName(filePath))
                        .firstCommitDate(date)
                        .build();
            }

            if (isWithinHotSpotAnalysisPeriod(date)) {
                fileInfo.incrementCommitsInHotSpotAnalysisPeriod();
            }
            if (isWithinLastMonth(date)) {
                fileInfo.incrementCommitsLastMonth();
                fileInfo.incrementCommitsLastYear();
            } else if (isWithinLastYear(date)) {
                fileInfo.incrementCommitsLastYear();
            }
            fileInfo.incrementTotalCommits();
            fileInfo.setLastCommitDate(date);
            return fileInfo;
        });
    }

    private String getFileName(String filePath) {
        String[] parts = filePath.replace("\\", "/").split("/");
        return parts[parts.length - 1];
    }

    public void updateFilePath(String oldPath, String newPath) {
        if (newPath == null || newPath.isBlank()) {
            fileInfos.remove(oldPath);
            return;
        }

        FileInfo fileInfo = fileInfos.remove(oldPath);
        if (fileInfo != null) {
            fileInfo.setFilePath(newPath);
            fileInfo.setFileName(getFileName(newPath));
            fileInfos.put(newPath, fileInfo);
        }
    }

    private boolean isWithinHotSpotAnalysisPeriod(LocalDate date) {
        return !date.isBefore(referenceDate.minusMonths(HOT_SPOT_ANALYSIS_PERIOD_MONTHS));
    }

    private boolean isWithinLastMonth(LocalDate date) {
        return !date.isBefore(referenceDate.minusMonths(1));
    }

    private boolean isWithinLastYear(LocalDate date) {
        return !date.isBefore(referenceDate.minusYears(1));
    }

}
