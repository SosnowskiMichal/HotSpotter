package pwr.zpi.hotspotter.repositoryanalysis.dto;

import java.util.List;

public record AnalysisStatisticsDTO(
        Integer authors,
        Integer activeAuthors,
        Integer commits,
        Integer files,
        Integer codeLines,
        Integer commentLines,
        Integer blankLines,
        List<FileTypeStatisticsDTO> fileTypeStatistics
) {
    public record FileTypeStatisticsDTO(
            String fileType,
            Integer files,
            Integer codeLines,
            Integer commentLines,
            Integer blankLines
    ) { }
}
