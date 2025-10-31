package pwr.zpi.hotspotter.sonar.model;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sonar_analysis_status")
@Data
public class SonarAnalysisStatus {
    @Id
    private String id;
    private String repoAnalysisId;
    @NonNull
    private String projectKey;
    @NonNull
    private String status; // PENDING, RUNNING, SUCCESS, FAILED
    private String message;
    private long startTime;
    private long endTime;

    public SonarAnalysisStatus(@NonNull String projectKey, @NonNull String status, String message) {
        this.projectKey = projectKey;
        this.status = status;
        this.message = message;
        this.startTime = System.currentTimeMillis();
    }
}
