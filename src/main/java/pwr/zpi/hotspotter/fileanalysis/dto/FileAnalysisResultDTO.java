package pwr.zpi.hotspotter.fileanalysis.dto;

import java.time.LocalDateTime;
import java.util.List;

public record FileAnalysisResultDTO(
        String analysisId,
        String filePath,
        LocalDateTime analysisStartedAt,
        LocalDateTime analysisFinishedAt,
        Long analysisTimeInSeconds,

        List<FileCommitDTO> commits,
        List<FileVersionStatisticsDTO> versionsStatistics,
        Integer versions,

        List<MethodStatisticsDTO> methodsStatistics,
        Integer methods,

        List<FileAuthorStatisticsDTO> currentAuthorsStatistics,
        Integer currentAuthors
) { }
