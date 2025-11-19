package pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.dto.LineStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.repository.FileInfoRepository;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.AnalysisStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.model.FileTypeStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.statistics.repository.AnalysisStatisticsRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisStatisticsCalculator {

    private final AnalysisStatisticsRepository analysisStatisticsRepository;
    private final FileInfoRepository fileInfoRepository;
    private final AuthorStatisticsRepository authorStatisticsRepository;

    public void calculateStatistics(String analysisId) {
        log.debug("Calculating analysis summary for analysis ID: {}", analysisId);

        int authors = authorStatisticsRepository.countAllByAnalysisId(analysisId);
        int activeAuthors = authorStatisticsRepository.countActiveAuthorsByAnalysisId(analysisId);
        int commits = authorStatisticsRepository.sumCommitsByAnalysisId(analysisId);

        int files = fileInfoRepository.countAllByAnalysisId(analysisId);
        LineStatistics lineStats = fileInfoRepository.getLineStatisticsByAnalysisId(analysisId);
        List<FileTypeStatistics> fileTypeSummaries = convertToFileTypeSummaries(
                fileInfoRepository.getFileTypeStatisticsByAnalysisId(analysisId)
        );

        AnalysisStatistics summary = AnalysisStatistics.builder()
                        .analysisId(analysisId)
                        .authors(authors)
                        .activeAuthors(activeAuthors)
                        .commits(commits)
                        .files(files)
                        .codeLines(lineStats != null ? lineStats.getCodeLines() : null)
                        .commentLines(lineStats != null ? lineStats.getCommentLines() : null)
                        .blankLines(lineStats != null ? lineStats.getBlankLines() : null)
                        .fileTypeSummaries(fileTypeSummaries)
                        .build();

        try {
            analysisStatisticsRepository.save(summary);
            log.debug("Saved analysis summary for ID: {}", analysisId);
        } catch (Exception e) {
            log.error("Error saving analysis summary ID: {}: {}", analysisId, e.getMessage(), e);
        }
    }

    private List<FileTypeStatistics> convertToFileTypeSummaries(List<pwr.zpi.hotspotter.repositoryanalysis.analyzer.fileinfo.dto.FileTypeStatistics> fileTypeStatistics) {
        return fileTypeStatistics.stream()
                .map(stats -> FileTypeStatistics.builder()
                        .fileType(stats.getFileType())
                        .files(stats.getFiles())
                        .codeLines(stats.getCodeLines())
                        .commentLines(stats.getCommentLines())
                        .blankLines(stats.getBlankLines())
                        .build())
                .toList();
    }

}
