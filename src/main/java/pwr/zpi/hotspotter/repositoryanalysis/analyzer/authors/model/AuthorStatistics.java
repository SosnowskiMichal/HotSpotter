package pwr.zpi.hotspotter.repositoryanalysis.analyzer.authors.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "author_statistics")
@CompoundIndex(name = "analysis_author_idx", def = "{'analysisId': 1, 'name': 1}", unique = true)
public class AuthorStatistics {

    @Id
    private String id;

    @NotBlank(message = "Analysis ID is required")
    private String analysisId;

    @NotBlank(message = "Name is required")
    private String name;

    @Builder.Default
    private Set<String> emails = new HashSet<>();

    @NotNull(message = "First commit date is required")
    private LocalDate firstCommitDate;

    @NotNull(message = "Last commit date is required")
    private LocalDate lastCommitDate;

    @Builder.Default
    private Boolean isActive = true;

    private Integer daysSinceLastCommit;

    private Integer monthsSinceLastCommit;

    private Integer daysSinceFirstCommit;

    private Integer monthsSinceFirstCommit;

    @Builder.Default
    private Integer commits = 0;

    @Builder.Default
    private Integer totalLinesAdded = 0;

    @Builder.Default
    private Integer totalLinesDeleted = 0;

    // TODO: Calculate
    @Builder.Default
    private Integer uniqueModifiedFiles = 0;

    @Builder.Default
    private Integer filesAsLeadAuthor = 0;

    public void addEmail(String email) {
        this.emails.add(email);
    }

    public void incrementCommits() {
        this.commits++;
    }

    public void increaseLinesAdded(int lines) {
        this.totalLinesAdded += lines;
    }

    public void increaseLinesDeleted(int lines) {
        this.totalLinesDeleted += lines;
    }

    public void incrementFilesAsLeadAuthor() {
        this.filesAsLeadAuthor++;
    }

}
