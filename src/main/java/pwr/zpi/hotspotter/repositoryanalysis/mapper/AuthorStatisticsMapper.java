package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model.AuthorStatistics;
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
                statistics.getMonthsSinceLastCommit(),
                statistics.getDaysSinceFirstCommit(),
                statistics.getMonthsSinceFirstCommit(),

                statistics.getCommits(),
                statistics.getTotalLinesAdded(),
                statistics.getTotalLinesDeleted(),

                statistics.getExistingFilesModified(),
                statistics.getFilesAsLeadAuthor()
        );
    }

    public AuthorSummaryDTO toSummaryDTO(AuthorStatistics statistics) {
        if (statistics == null) return null;

        return new AuthorSummaryDTO(
                statistics.getName(),
                statistics.getEmails(),
                statistics.getIsActive()
        );
    }

}
