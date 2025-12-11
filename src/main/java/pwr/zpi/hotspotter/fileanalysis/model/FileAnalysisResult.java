package pwr.zpi.hotspotter.fileanalysis.model;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import pwr.zpi.hotspotter.fileanalysis.blame.model.FileAuthorStatistics;
import pwr.zpi.hotspotter.fileanalysis.logprocessing.model.FileCommit;
import pwr.zpi.hotspotter.fileanalysis.methods.model.MethodStatistics;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "file_analysis_results")
@CompoundIndex(name = "analysis_file_idx", def = "{'analysisId' : 1, 'filePath': 1}", unique = true)
public class FileAnalysisResult {

    @Id
    private String id;

    @NotNull(message = "Analysis ID is required")
    private String analysisId;

    @NotNull(message = "File path is required")
    private String filePath;

    @Builder.Default
    @NotNull(message = "Analysis start time is required")
    private LocalDateTime analysisStartedAt = LocalDateTime.now();

    private LocalDateTime analysisFinishedAt;

    private Long analysisTimeInSeconds;

    private List<FileCommit> fileCommits;

    private Integer totalFileVersions;

    private List<FileVersionStatistics> fileVersionStatistics;

    private List<MethodStatistics> methodStatistics;

    private Integer numberOfCurrentAuthors;

    private List<FileAuthorStatistics> currentAuthors;

    @Builder.Default
    private FileAnalysisStatus status = FileAnalysisStatus.IN_PROGRESS;

    public void markAsCompleted() {
        this.status = FileAnalysisStatus.COMPLETED;
        this.analysisFinishedAt = LocalDateTime.now();
        this.analysisTimeInSeconds = Duration.between(analysisStartedAt, analysisFinishedAt).getSeconds();
    }

    public enum FileAnalysisStatus {
        IN_PROGRESS,
        COMPLETED,
    }

}
