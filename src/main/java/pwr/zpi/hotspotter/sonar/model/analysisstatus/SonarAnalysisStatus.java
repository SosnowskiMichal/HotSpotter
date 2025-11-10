package pwr.zpi.hotspotter.sonar.model.analysisstatus;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "sonar_analysis_status")
@Data
public class SonarAnalysisStatus {
    @Id
    private String id;
    @NonNull
    private String repoAnalysisId;
    @NonNull
    private String projectKey;
    @NonNull
    private SonarAnalysisState status;
    private String message;
    private long startTime;
    private long endTime;

    public SonarAnalysisStatus(@NonNull String repoAnalysisId, @NonNull String projectKey, @NonNull SonarAnalysisState status, String message) {
        this.repoAnalysisId = repoAnalysisId;
        this.projectKey = projectKey;
        this.status = status;
        this.message = message;
        this.startTime = System.currentTimeMillis();
    }

    @SuppressWarnings("unused")
    public SonarAnalysisStatus() {
        this.startTime = System.currentTimeMillis();
    }
}
