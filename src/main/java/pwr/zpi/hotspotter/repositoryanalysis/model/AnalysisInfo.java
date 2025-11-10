package pwr.zpi.hotspotter.repositoryanalysis.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "analyses")
public class AnalysisInfo {

    @Id
    private String id;

    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    @NotBlank(message = "Repository name is required")
    private String repositoryName;

    @NotBlank(message = "Repository owner is required")
    private String repositoryOwner;

    @Builder.Default
    @NotNull(message = "Analysis date is required")
    private LocalDateTime analyzedAt = LocalDateTime.now();

    @Builder.Default
    @NotNull(message = "Analysis status is required")
    private AnalysisStatus status = AnalysisStatus.IN_PROGRESS;

    private Long analysisTimeInSeconds;

    private LocalDate startDate;

    private LocalDate endDate;

    public enum AnalysisStatus {
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public enum AnalysisSseStatus {
        DOWNLOADING,
        PROCESSING_DATA,
        ANALYZING,
        SONAR
    }

    public void markAsCompleted() {
        this.status = AnalysisStatus.COMPLETED;
    }

    public void markAsFailed() {
        this.status = AnalysisStatus.FAILED;
    }

}
