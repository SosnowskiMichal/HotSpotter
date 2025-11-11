package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/*public class ActivityTrendsDailyStats{
        LocalDate date,
        long commitsCount,
        int uniqueAuthorsCount,
        int activeAuthorsCount,
        int linesAdded,
        int linesDeleted
) {}*/

@Data
@Builder
public class ActivityTrendsDailyStats  {
    private LocalDate date;
    private long commitsCount;
    private int uniqueAuthorsCount;
    private int activeAuthorsCount;
    private int linesAdded;
    private int linesDeleted;
}

