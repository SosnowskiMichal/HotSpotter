package pwr.zpi.hotspotter.repositoryanalysis.mapper;

import org.springframework.stereotype.Component;
import pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model.ActivityTrendsDailyStats;
import pwr.zpi.hotspotter.repositoryanalysis.dto.DailyStatsDTO;

@Component
public class DailyStatsMapper {

    public DailyStatsDTO toDTO(ActivityTrendsDailyStats dailyStats) {
        if (dailyStats == null) return null;

        return new DailyStatsDTO(
                dailyStats.getDate(),
                dailyStats.getCommits(),
                dailyStats.getUniqueAuthors(),
                dailyStats.getActiveAuthors(),
                dailyStats.getLinesAdded(),
                dailyStats.getLinesDeleted()
        );
    }

}
