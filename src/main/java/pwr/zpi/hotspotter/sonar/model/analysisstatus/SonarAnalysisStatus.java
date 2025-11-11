package pwr.zpi.hotspotter.sonar.model.analysisstatus;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public SonarAnalysisStatus(@NonNull String repoAnalysisId, @NonNull String projectKey, @NonNull SonarAnalysisState status, String message) {
        this.repoAnalysisId = repoAnalysisId;
        this.projectKey = projectKey;
        this.status = status;
        this.message = message;
        this.startTime = LocalDateTime.now();
    }

    @SuppressWarnings("unused")
    public SonarAnalysisStatus() {
        this.startTime = LocalDateTime.now();
    }
}
