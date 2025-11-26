package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.repository.AuthorStatisticsRepository.*;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AuthorStatisticsDTO;
import pwr.zpi.hotspotter.repositoryanalysis.dto.AuthorSummaryDTO;

@Component
public class AuthorStatisticsMapper {

    public AuthorStatisticsDTO toDTO(AuthorStatistics statistics) {
        if (statistics == null) return null;

        return new AuthorStatisticsDTO(
                statistics.getName(),
                statistics.getEmails(),

                statistics.getFirstCommitDate(),
                statistics.getLastCommitDate(),
                statistics.getIsActive(),

                statistics.getDaysSinceLastCommit(),
                statistics.getDaysSinceFirstCommit(),

                statistics.getCommits(),
                statistics.getTotalLinesAdded(),
                statistics.getTotalLinesDeleted(),

                statistics.getExistingFilesModified(),
                statistics.getFilesAsLeadAuthor()
        );
    }

    public AuthorSummaryDTO toSummaryDTO(AuthorSummaryProjection projection) {
        if (projection == null) return null;

        return new AuthorSummaryDTO(
                projection.getName(),
                projection.getEmails(),
                projection.getIsActive()
        );
    }

}
