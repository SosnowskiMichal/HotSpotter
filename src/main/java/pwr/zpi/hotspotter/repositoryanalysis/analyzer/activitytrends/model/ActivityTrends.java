package pwr.zpi.hotspotter.repositoryanalysis.analyzer.activitytrends.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activity_trends")
public class ActivityTrends {

    @Id
    private String analysisId;

    private List<ActivityTrendsDailyStats> dailyStats;

}
