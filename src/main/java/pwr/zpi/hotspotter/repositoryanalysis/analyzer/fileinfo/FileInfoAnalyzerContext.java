package pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo;

import lombok.Getter;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.model.FileInfo;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Getter
public class FileInfoAnalyzerContext {

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
                        .build();
            }

            if (isWithinLastMonth(date)) {
                fileInfo.incrementCommitsLastMonth();
                fileInfo.incrementCommitsLastYear();
            } else if (isWithinLastYear(date)) {
                fileInfo.incrementCommitsLastYear();
            }
            fileInfo.setLastCommitDate(date);
            return fileInfo;
        });
    }

    public void updateFilePath(String oldPath, String newPath) {
        if (newPath == null || newPath.isBlank()) {
            fileInfos.remove(oldPath);
            return;
        }

        FileInfo fileInfo = fileInfos.remove(oldPath);
        if (fileInfo != null) {
            fileInfo.setFilePath(newPath);
            fileInfos.put(newPath, fileInfo);
        }
    }

    private boolean isWithinLastMonth(LocalDate date) {
        return !date.isBefore(referenceDate.minusMonths(1));
    }

    private boolean isWithinLastYear(LocalDate date) {
        return !date.isBefore(referenceDate.minusYears(1));
    }

}
