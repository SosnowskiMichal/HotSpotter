package pwr.zpi.hotspotter.fileanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.fileanalysis.blame.model.FileAuthorStatistics;
import pwr.zpi.hotspotter.fileanalysis.dto.*;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;
import pwr.zpi.hotspotter.fileanalysis.methods.model.MethodStatistics;
import pwr.zpi.hotspotter.fileanalysis.methods.model.MethodVersionStatistics;
import pwr.zpi.hotspotter.fileanalysis.model.FileAnalysisResult;
import pwr.zpi.hotspotter.fileanalysis.model.FileVersionStatistics;

import java.util.List;

@Component
public class FileAnalysisResultMapper {

    private static final int METHOD_STATISTICS_MAX_SIZE = 50;

    public FileAnalysisResultDTO toDTO(FileAnalysisResult fileAnalysisResult) {
        if (fileAnalysisResult == null) return null;

        List<FileCommitDTO> fileCommitDTOs = fileAnalysisResult.getFileCommits().stream()
                .map(this::toFileCommitDTO)
                .toList();

        List<FileVersionStatisticsDTO> fileVersionStatisticsDTOs = fileAnalysisResult.getFileVersionStatistics().stream()
                .map(this::toFileVersionStatisticsDTO)
                .toList();

        List<MethodStatisticsDTO> methodStatisticsDTOs = fileAnalysisResult.getMethodStatistics().stream()
                .sorted((method1, method2) -> Integer.compare(method2.getCommits(), method1.getCommits()))
                .limit(METHOD_STATISTICS_MAX_SIZE)
                .map(this::toMethodStatisticsDTO)
                .toList();

        List<FileAuthorStatisticsDTO> currentAuthorStatisticsDTOs = fileAnalysisResult.getCurrentAuthors().stream()
                .map(this::toFileAuthorStatisticsDTO)
                .toList();

        return new FileAnalysisResultDTO(
                fileAnalysisResult.getAnalysisId(),
                fileAnalysisResult.getFilePath(),
                fileAnalysisResult.getAnalysisStartedAt(),
                fileAnalysisResult.getAnalysisFinishedAt(),
                fileAnalysisResult.getAnalysisTimeInSeconds(),

                fileCommitDTOs,
                fileVersionStatisticsDTOs,
                fileVersionStatisticsDTOs.size(),

                methodStatisticsDTOs,
                fileAnalysisResult.getNumberOfMethods(),

                currentAuthorStatisticsDTOs,
                currentAuthorStatisticsDTOs.size()
        );
    }

    private FileCommitDTO toFileCommitDTO(FileCommit fileCommit) {
        return new FileCommitDTO(
                fileCommit.hash(),
                fileCommit.date(),
                fileCommit.author(),
                fileCommit.linesAdded(),
                fileCommit.linesDeleted()
        );
    }

    private FileVersionStatisticsDTO toFileVersionStatisticsDTO(FileVersionStatistics fileVersionStatistics) {
        return new FileVersionStatisticsDTO(
                fileVersionStatistics.getDate(),
                fileVersionStatistics.getUrl(),
                fileVersionStatistics.getTotalLines(),
                fileVersionStatistics.getCodeLines(),
                fileVersionStatistics.getCommentLines(),
                fileVersionStatistics.getBlankLines(),
                fileVersionStatistics.getComplexity(),
                fileVersionStatistics.getNumberOfMethods()
        );
    }

    private MethodStatisticsDTO toMethodStatisticsDTO(MethodStatistics methodStatistics) {
        List<MethodStatisticsDTO.MethodVersionStatisticsDTO> versionStatisticsDTOs = methodStatistics
                .getComplexityTrends().stream()
                .map(this::toMethodVersionStatisticsDTO)
                .toList();

        return new MethodStatisticsDTO(
                methodStatistics.getName(),
                methodStatistics.getStartLine(),
                methodStatistics.getEndLine(),
                methodStatistics.getLines(),
                methodStatistics.getUrl(),
                methodStatistics.getCommits(),
                methodStatistics.getAuthors(),
                methodStatistics.getFirstCommitDate(),
                methodStatistics.getLastCommitDate(),
                methodStatistics.getDaysSinceLastCommit(),
                versionStatisticsDTOs
        );
    }

    private MethodStatisticsDTO.MethodVersionStatisticsDTO toMethodVersionStatisticsDTO(
            MethodVersionStatistics methodVersionStatistics
    ) {
        return new MethodStatisticsDTO.MethodVersionStatisticsDTO(
                methodVersionStatistics.date(),
                methodVersionStatistics.complexity(),
                methodVersionStatistics.lines()
        );
    }

    private FileAuthorStatisticsDTO toFileAuthorStatisticsDTO(FileAuthorStatistics fileAuthorStatistics) {
        return new FileAuthorStatisticsDTO(
                fileAuthorStatistics.getAuthorName(),
                fileAuthorStatistics.getLinesAuthored(),
                fileAuthorStatistics.getPercentage()
        );
    }

}
