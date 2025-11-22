package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.AnalysisStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.FileTypeStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AnalysisStatisticsDTO;

import java.util.List;

@Component
public class AnalysisStatisticsMapper {

    public AnalysisStatisticsDTO toDTO(AnalysisStatistics statistics) {
        if (statistics == null) return null;

        List<AnalysisStatisticsDTO.FileTypeStatisticsDTO> fileTypeStatisticsDTOs = statistics.getFileTypeSummaries().stream()
                .map(this::toFileTypeStatisticsDTO)
                .toList();

        return new AnalysisStatisticsDTO(
                statistics.getAuthors(),
                statistics.getActiveAuthors(),
                statistics.getCommits(),
                statistics.getFiles(),
                statistics.getCodeLines(),
                statistics.getCommentLines(),
                statistics.getBlankLines(),
                fileTypeStatisticsDTOs
        );
    }

    private AnalysisStatisticsDTO.FileTypeStatisticsDTO toFileTypeStatisticsDTO(FileTypeStatistics statistics) {
        return new AnalysisStatisticsDTO.FileTypeStatisticsDTO(
                statistics.getFileType(),
                statistics.getFiles(),
                statistics.getCodeLines(),
                statistics.getCommentLines(),
                statistics.getBlankLines()
        );
    }

}
