package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityTrendsDailyStats {

    private LocalDate date;

    @Builder.Default
    private Integer commits = 0;

    private Integer uniqueAuthors;

    private Integer activeAuthors;

    @Builder.Default
    private Integer linesAdded = 0;

    @Builder.Default
    private Integer linesDeleted = 0;

    public void incrementCommits() {
        this.commits++;
    }

    public void increaseLinesAdded(int lines) {
        this.linesAdded += lines;
    }

    public void increaseLinesDeleted(int lines) {
        this.linesDeleted += lines;
    }

}
