package pwr.zpi.hotspotter.repositorymanagement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "repositories")
public class RepositoryInfo {

    @Id
    private String id;

    @NotBlank(message = "Remote URL is required")
    @Indexed(unique = true)
    private String remoteUrl;

    @NotBlank(message = "Local path is required")
    private String localPath;

    @NotNull
    private LocalDateTime clonedAt;

    @NotNull
    private LocalDateTime lastAccessedAt;

    @NotNull
    private Integer accessCount;

    private Long sizeInBytes;

    public RepositoryInfo(String remoteUrl, String localPath) {
        this.remoteUrl = remoteUrl;
        this.localPath = localPath;
        this.clonedAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount = 0;
    }

    public void recordUsage() {
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount += 1;
    }

}
