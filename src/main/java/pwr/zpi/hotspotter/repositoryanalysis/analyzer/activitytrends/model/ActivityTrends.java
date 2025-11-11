package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@Document(collection = "activity_trends")
public class ActivityTrends {
    @Id
    private String id;
    private String analysisId;
    private List<ActivityTrendsDailyStats> dailyStats;
}
