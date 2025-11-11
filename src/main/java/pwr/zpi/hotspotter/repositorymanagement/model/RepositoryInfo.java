package pwr.zpi.hotspotter.repositorymanagement.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import pwr.zpi.hotspotter.repositorymanagement.parser.RepositoryUrlParser;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "repositories")
public class RepositoryInfo {

    @Id
    private String id;

    @NotBlank(message = "Remote URL is required")
    private String remoteUrl;

    @NotBlank(message = "Repository name is required")
    private String name;

    @NotBlank(message = "Repository owner is required")
    private String owner;

    @NotBlank(message = "Repository platform is required")
    private String platform;

    @NotBlank(message = "Local path is required")
    private String localPath;

    @NotNull
    private LocalDateTime clonedAt;

    @NotNull
    private LocalDateTime lastAccessedAt;

    @NotNull
    private Integer accessCount;

    private Long sizeInBytes;

    public RepositoryInfo(RepositoryUrlParser.RepositoryData repositoryData, String localPath) {
        this.remoteUrl = repositoryData.repositoryUrl();
        this.name = repositoryData.name();
        this.owner = repositoryData.owner();
        this.platform = repositoryData.platform();
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
